const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => [...document.querySelectorAll(selector)];
const KEY = "fleasion-mobile-v1";
const THEME_KEY = "fleasion-mobile-theme";
const makeId = () => (globalThis.crypto && crypto.randomUUID ? crypto.randomUUID() : "p-" + Date.now().toString(36) + Math.random().toString(36).slice(2));

function createStarter() {
  return { version: 1, profiles: [{ id: makeId(), name: "Default profile", rules: [] }], active: 0 };
}
function readLocal(key) {
  try { return localStorage.getItem(key); } catch { persistenceAvailable = false; return null; }
}

let persistenceAvailable = true;
let startupWarning = "";
let parsed = null;
try {
  parsed = JSON.parse(readLocal(KEY) || "null");
  if (parsed && typeof parsed !== "object") throw new Error("Invalid saved data");
} catch {
  startupWarning = "Saved data could not be read. A fresh profile was opened; export a backup before continuing if you have one.";
  parsed = null;
  persistenceAvailable = false;
}

function normalizeData(value) {
  if (!value || !Array.isArray(value.profiles) || value.profiles.length === 0) return createStarter();
  const profiles = value.profiles.map((profile, index) => ({
    id: String((profile && profile.id) || makeId()),
    name: String((profile && profile.name) || ("Profile " + (index + 1))).slice(0, 60),
    rules: Array.isArray(profile && profile.rules) ? profile.rules.map((rule) => ({
      originalId: String((rule && (rule.originalId ?? rule.original_id ?? rule.from)) ?? ""),
      replacement: String((rule && (rule.replacement ?? rule.to)) ?? ""),
      notes: String((rule && rule.notes) ?? "").slice(0, 120),
      enabled: !rule || rule.enabled !== false
    })) : []
  }));
  const active = Number.isInteger(value.active) && profiles[value.active] ? value.active : 0;
  return { version: 1, profiles, active };
}

let data = normalizeData(parsed);
let editingProfile = null;
let editingRule = null;
let toastTimer = null;

function toast(message) {
  const element = $("#toast");
  element.textContent = message;
  element.classList.add("show");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => element.classList.remove("show"), 2600);
}
function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, (character) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  })[character]);
}
function current() {
  return data.profiles[data.active] || data.profiles[0];
}
function save() {
  try {
    localStorage.setItem(KEY, JSON.stringify(data));
    persistenceAvailable = true;
  } catch {
    persistenceAvailable = false;
  }
  render();
  if (!persistenceAvailable) toast("Changes are only in this open session. Browser storage is unavailable.");
  return persistenceAvailable;
}

function render() {
  const totalRules = data.profiles.reduce((count, profile) => count + profile.rules.length, 0);
  const enabledRules = data.profiles.reduce((count, profile) => count + profile.rules.filter((rule) => rule.enabled !== false).length, 0);
  $("#profileStat").textContent = String(data.profiles.length);
  $("#ruleStat").textContent = String(enabledRules);

  $("#profileList").innerHTML = data.profiles.map((profile, index) =>
    "<div class='item " + (index === data.active ? "selected" : "") + "'>" +
      "<div class='itemcopy'><b>" + escapeHtml(profile.name) + "</b>" +
      "<small>" + profile.rules.length + " rule" + (profile.rules.length === 1 ? "" : "s") + " · " +
      profile.rules.filter((rule) => rule.enabled !== false).length + " enabled " + (index === data.active ? "· ACTIVE" : "") + "</small></div>" +
      "<div class='itembuttons'>" +
      "<button type='button' class='secondary' data-select='" + index + "' aria-pressed='" + (index === data.active) + "'>" + (index === data.active ? "Active" : "Use") + "</button>" +
      "<button type='button' class='secondary' data-edit-profile='" + index + "' aria-label='Rename " + escapeHtml(profile.name) + "'>✎</button>" +
      "</div></div>"
  ).join("");

  const query = ($("#ruleSearch").value || "").trim().toLowerCase();
  const rules = current().rules.map((rule, index) => ({ ...rule, _index: index }))
    .filter((rule) => !query || [rule.originalId, rule.replacement, rule.notes].some((value) => String(value || "").toLowerCase().includes(query)));

  $("#ruleCount").textContent = current().rules.length + " total · " + current().rules.filter((rule) => rule.enabled !== false).length + " enabled";
  $("#ruleList").innerHTML = rules.map((rule) =>
    "<div class='item " + (rule.enabled === false ? "rule-disabled" : "") + "'>" +
      "<div class='rulecopy'><b>" + escapeHtml(rule.originalId) + " → " + escapeHtml(rule.replacement) + "</b>" +
      "<small>" + escapeHtml(rule.notes || "No notes") + " · " + (rule.enabled === false ? "Disabled" : "Enabled") + "</small></div>" +
      "<div class='itembuttons'>" +
      "<button type='button' class='secondary' data-toggle-rule='" + rule._index + "'>" + (rule.enabled === false ? "Enable" : "Disable") + "</button>" +
      "<button type='button' class='secondary' data-edit-rule='" + rule._index + "' aria-label='Edit rule for " + escapeHtml(rule.originalId) + "'>✎</button>" +
      "<button type='button' class='danger' data-delete-rule='" + rule._index + "' aria-label='Delete rule for " + escapeHtml(rule.originalId) + "'>×</button>" +
      "</div></div>"
  ).join("") || "<div class='empty'>" + (query ? "No matching rules." : "No rules in this profile yet. Add one to get started.") + "</div>";

  $("#storageInfo").textContent = persistenceAvailable
    ? data.profiles.length + " profile(s) · " + totalRules + " rule(s) saved on this device."
    : data.profiles.length + " profile(s) · " + totalRules + " rule(s) in this session only. Browser storage is unavailable.";
}

function openDialog(dialog) {
  if (typeof dialog.showModal === "function") dialog.showModal();
  else dialog.setAttribute("open", "");
}

$$(".tab").forEach((button) => button.addEventListener("click", () => {
  $$(".tab").forEach((tab) => {
    const active = tab === button;
    tab.classList.toggle("active", active);
    tab.setAttribute("aria-selected", String(active));
  });
  $$(".panel").forEach((panel) => panel.classList.toggle("active", panel.id === button.dataset.tab));
}));

const menuButton = $("#menuBtn");
const appNav = $("#appNav");
function setMenuOpen(open) {
  appNav.hidden = !open;
  menuButton.setAttribute("aria-expanded", String(open));
  menuButton.setAttribute("aria-label", open ? "Close navigation menu" : "Open navigation menu");
  menuButton.textContent = open ? "✕" : "☰";
}
menuButton.addEventListener("click", () => setMenuOpen(appNav.hidden));
document.addEventListener("keydown", event => {
  if (event.key === "Escape" && !appNav.hidden) setMenuOpen(false);
});
document.addEventListener("click", event => {
  if (!appNav.hidden && !appNav.contains(event.target) && !menuButton.contains(event.target)) setMenuOpen(false);
});
$(".tab").forEach(button => button.addEventListener("click", () => setMenuOpen(false)));

$("#newProfile").addEventListener("click", () => {
  editingProfile = null;
  $("#profileDialogTitle").textContent = "New profile";
  $("#profileName").value = "";
  openDialog($("#profileDialog"));
  $("#profileName").focus();
});

$("#profileList").addEventListener("click", (event) => {
  const selectButton = event.target.closest("[data-select]");
  const editButton = event.target.closest("[data-edit-profile]");
  if (selectButton) {
    data.active = Number(selectButton.dataset.select);
    save();
  }
  if (editButton) {
    editingProfile = Number(editButton.dataset.editProfile);
    $("#profileDialogTitle").textContent = "Rename profile";
    $("#profileName").value = data.profiles[editingProfile].name;
    openDialog($("#profileDialog"));
    $("#profileName").focus();
  }
});

$("#profileForm").addEventListener("submit", (event) => {
  event.preventDefault();
  const name = $("#profileName").value.trim();
  if (!name) return;
  if (editingProfile === null) {
    data.profiles.push({ id: makeId(), name, rules: [] });
    data.active = data.profiles.length - 1;
  } else if (data.profiles[editingProfile]) {
    data.profiles[editingProfile].name = name;
  }
  $("#profileDialog").close();
  if (save()) toast("Profile saved");
});

$("#newRule").addEventListener("click", () => {
  editingRule = null;
  $("#ruleDialogTitle").textContent = "Add replacement";
  $("#originalId").value = "";
  $("#replacement").value = "";
  $("#ruleNotes").value = "";
  openDialog($("#ruleDialog"));
  $("#originalId").focus();
});

$("#ruleSearch").addEventListener("input", render);

$("#ruleList").addEventListener("click", (event) => {
  const editButton = event.target.closest("[data-edit-rule]");
  const deleteButton = event.target.closest("[data-delete-rule]");
  const toggleButton = event.target.closest("[data-toggle-rule]");

  if (editButton) {
    editingRule = Number(editButton.dataset.editRule);
    const rule = current().rules[editingRule];
    if (!rule) return;
    $("#ruleDialogTitle").textContent = "Edit replacement";
    $("#originalId").value = rule.originalId;
    $("#replacement").value = rule.replacement;
    $("#ruleNotes").value = rule.notes || "";
    openDialog($("#ruleDialog"));
    $("#originalId").focus();
  }
  if (toggleButton) {
    const rule = current().rules[Number(toggleButton.dataset.toggleRule)];
    if (!rule) return;
    rule.enabled = rule.enabled === false;
    if (save()) toast(rule.enabled ? "Rule enabled" : "Rule disabled");
  }
  if (deleteButton) {
    const index = Number(deleteButton.dataset.deleteRule);
    const rule = current().rules[index];
    if (!rule) return;
    if (confirm("Delete the rule for asset " + rule.originalId + "? This cannot be undone.")) {
      current().rules.splice(index, 1);
      if (save()) toast("Rule deleted");
    } else {
      toast("Deletion cancelled");
    }
  }
});

function validReplacementTarget(value) {
  const target = String(value || "").trim();
  if (!target || target.length > 500) return false;
  if (/^[0-9]+$/.test(target)) return true;
  if (/^[a-z][a-z0-9+.-]*:/i.test(target)) {
    try {
      const parsedUrl = new URL(target);
      return parsedUrl.protocol === "https:" && Boolean(parsedUrl.hostname);
    } catch {
      return false;
    }
  }
  return !target.startsWith("//") && !target.includes(String.fromCharCode(92)) && !target.split("/").includes("..");
}
function normalizeRule(rule) {
  const originalId = String((rule && (rule.originalId ?? rule.original_id ?? rule.from)) ?? "").trim();
  const replacement = String((rule && (rule.replacement ?? rule.to)) ?? "").trim();
  if (!/^[0-9]+$/.test(originalId)) throw new Error("Every original asset ID must contain digits only.");
  if (!validReplacementTarget(replacement)) throw new Error("Use a numeric ID, an HTTPS URL, or a relative local path as the replacement target.");
  return {
    originalId,
    replacement,
    notes: String((rule && rule.notes) ?? "").trim().slice(0, 120),
    enabled: !rule || rule.enabled !== false
  };
}

$("#ruleForm").addEventListener("submit", (event) => {
  event.preventDefault();
  let rule;
  try {
    rule = normalizeRule({
      originalId: $("#originalId").value,
      replacement: $("#replacement").value,
      notes: $("#ruleNotes").value,
      enabled: editingRule === null ? true : current().rules[editingRule]?.enabled !== false
    });
  } catch (error) {
    toast(error.message);
    return;
  }
  if (editingRule === null) {
    current().rules.push(rule);
  } else if (current().rules[editingRule]) {
    current().rules[editingRule] = rule;
  } else {
    toast("That rule is no longer available. Please add it again.");
    return;
  }
  $("#ruleDialog").close();
  if (save()) toast("Replacement rule saved");
});

function downloadJson(value, filename) {
  if (window.FleasionNative && typeof window.FleasionNative.saveJsonFile === "function") {
    window.FleasionNative.saveJsonFile(filename, JSON.stringify(value, null, 2));
    return;
  }
  const blob = new Blob([JSON.stringify(value, null, 2)], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.append(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
$("#exportConfig").addEventListener("click", () => {
  const profile = current();
  const filename = profile.name.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "") || "fleasion-profile";
  downloadJson({
    version: 3,
    profile: profile.name,
    replacements: profile.rules.map((rule) => ({
      originalId: rule.originalId,
      replacement: rule.replacement,
      notes: rule.notes || "",
      enabled: rule.enabled !== false
    }))
  }, filename + ".json");
  toast("Profile export prepared");
});

$("#importConfig").addEventListener("click", () => $("#fileInput").click());
$("#fileInput").addEventListener("change", async (event) => {
  const file = event.target.files && event.target.files[0];
  if (!file) return;
  try {
    if (file.size > 2 * 1024 * 1024) throw new Error("Choose a JSON file smaller than 2 MB.");
    const json = JSON.parse(await file.text());
    const sourceRules = json.replacements ?? json.rules;
    if (!Array.isArray(sourceRules)) throw new Error("The JSON must contain a rules or replacements array.");
    const rules = sourceRules.map(normalizeRule);
    const name = String(json.profile ?? json.name ?? file.name.replace(/[.]json$/i, "")).trim().slice(0, 60) || "Imported profile";
    data.profiles.push({ id: makeId(), name, rules });
    data.active = data.profiles.length - 1;
    if (save()) toast("Profile imported");
  } catch (error) {
    toast("Import failed: " + (error && error.message ? error.message : "Invalid JSON file"));
  } finally {
    event.target.value = "";
  }
});

$("#validateBtn").addEventListener("click", () => $("#validateInput").click());
$("#validateInput").addEventListener("change", async (event) => {
  const file = event.target.files && event.target.files[0];
  if (!file) return;
  try {
    if (file.size > 2 * 1024 * 1024) throw new Error("Choose a JSON file smaller than 2 MB.");
    JSON.parse(await file.text());
    toast("Valid JSON ✓");
  } catch (error) {
    toast("Invalid JSON: " + (error && error.message ? error.message : "Could not read file"));
  } finally {
    event.target.value = "";
  }
});

$("#resetBtn").addEventListener("click", () => {
  if (confirm("Delete all profiles stored by this app? Export a backup first if you need one.")) {
    data = createStarter();
    if (save()) toast("App data reset");
  }
});

let isLight = readLocal(THEME_KEY) === "light";
function applyTheme() {
  document.body.classList.toggle("light", isLight);
  $("#themeBtn").setAttribute("aria-pressed", String(isLight));
  $("#themeBtn").setAttribute("aria-label", "Switch to " + (isLight ? "dark" : "light") + " theme");
  $("#themeBtn").textContent = isLight ? "☾" : "☼";
}
$("#themeBtn").addEventListener("click", () => {
  isLight = !isLight;
  applyTheme();
  try { localStorage.setItem(THEME_KEY, isLight ? "light" : "dark"); } catch { /* Keep the theme for this session. */ }
  toast((isLight ? "Light" : "Dark") + " theme");
});

const fpsGuides = {
  low: "Low-end: set Roblox Graphics Mode to Manual and lower the in-game graphics quality; close unused apps, keep some free storage, and avoid playing while the phone is hot.",
  balanced: "Balanced: start with medium in-game graphics, close heavy background apps, use a stable connection, and lower graphics if frame pacing becomes uneven.",
  performance: "Performance priority: use the lowest graphics level that still looks clear, close background downloads, keep the phone ventilated, and avoid battery-saving modes that throttle performance."
};
applyTheme();
$("#fpsTips").textContent = fpsGuides[$("#fpsProfile").value];
$("#fpsProfile").addEventListener("change", () => {
  $("#fpsTips").textContent = fpsGuides[$("#fpsProfile").value] || fpsGuides.balanced;
});
let fpsTestRunning = false;
let fpsAnimationId = 0;
$("#fpsTestBtn").addEventListener("click", () => {
  if (fpsTestRunning) return;
  fpsTestRunning = true;
  const button = $("#fpsTestBtn");
  const result = $("#fpsResult");
  const label = $("#fpsResultLabel");
  button.disabled = true;
  button.textContent = "Testing…";
  result.textContent = "…";
  label.textContent = "Warming up the browser animation test";
  let started = 0;
  let frames = 0;
  let previousFrame = 0;
  let totalFrameTime = 0;
  function finish(message) {
    fpsTestRunning = false;
    cancelAnimationFrame(fpsAnimationId);
    button.disabled = false;
    button.textContent = "Run again";
    if (message) {
      result.textContent = "—";
      label.textContent = message;
    }
  }
  function sample(now) {
    if (document.visibilityState === "hidden") {
      finish("Test paused because the app was hidden");
      button.textContent = "Run 5-second test";
      return;
    }
    if (!started) {
      started = now;
      previousFrame = now;
      fpsAnimationId = requestAnimationFrame(sample);
      return;
    }
    totalFrameTime += now - previousFrame;
    previousFrame = now;
    frames += 1;
    const elapsed = now - started;
    if (elapsed >= 5000) {
      const measured = Math.max(0, Math.round(frames * 1000 / elapsed));
      const averageFrameTime = frames ? (totalFrameTime / frames).toFixed(1) : "—";
      result.textContent = measured + " FPS";
      label.textContent = "Avg. frame time: " + averageFrameTime + " ms · browser only";
      finish();
      return;
    }
    fpsAnimationId = requestAnimationFrame(sample);
  }
  fpsAnimationId = requestAnimationFrame(sample);
});
document.addEventListener("visibilitychange", () => {
  if (fpsTestRunning && document.visibilityState === "hidden") {
    fpsTestRunning = false;
    cancelAnimationFrame(fpsAnimationId);
    $("#fpsTestBtn").disabled = false;
    $("#fpsTestBtn").textContent = "Run 5-second test";
    $("#fpsResult").textContent = "—";
    $("#fpsResultLabel").textContent = "Test paused because the app was hidden";
  }
});

render();
if (startupWarning) toast(startupWarning);
if ("serviceWorker" in navigator) {
  window.addEventListener("load", () => navigator.serviceWorker.register("./sw.js").catch(() => {
    toast("Offline update could not be registered. The app still works online.");
  }));
}

// Fleasion 0.6.0 device snapshot. Browser-exposed values only; no privileged claims.
function refreshDeviceSnapshot() {
  const ua = navigator.userAgent || "";
  let platform = "Unknown browser";
  if (/Android/i.test(ua)) platform = "Android";
  else if (/iPhone|iPad|iPod/i.test(ua)) platform = "iOS";
  else if (/Windows/i.test(ua)) platform = "Windows";
  else if (/Mac OS/i.test(ua)) platform = "macOS";
  else if (/Linux/i.test(ua)) platform = "Linux";
  $("#devicePlatform").textContent = platform;
  $("#deviceCores").textContent = navigator.hardwareConcurrency ? String(navigator.hardwareConcurrency) : "Unavailable";
  $("#deviceMemory").textContent = navigator.deviceMemory ? "About " + navigator.deviceMemory + " GB" : "Not exposed";
  $("#deviceScreen").textContent = window.screen ? screen.width + " × " + screen.height + " · " + (window.devicePixelRatio || 1) + "×" : "Unavailable";
}
const refreshDeviceButton = $("#refreshDevice");
if (refreshDeviceButton) refreshDeviceButton.addEventListener("click", () => { refreshDeviceSnapshot(); refreshNativeDeviceSnapshot(); toast("Device snapshot refreshed"); });
refreshDeviceSnapshot();

function refreshNativeDeviceSnapshot() {
  if (!window.FleasionNative || typeof window.FleasionNative.getDeviceInfo !== "function") return;
  try {
    const info = JSON.parse(window.FleasionNative.getDeviceInfo());
    const fields = { devicePlatform: info.platform, deviceCores: info.cores, deviceMemory: info.memoryHint, deviceScreen: info.screen, deviceModel: info.model, deviceAndroid: info.android, deviceTotalRam: info.totalRam, deviceAvailableRam: info.availableRam, deviceRefresh: info.refreshRate, deviceStorage: info.freeStorage, deviceThermal: info.thermal };
    for (const [id, value] of Object.entries(fields)) { const node = document.getElementById(id); if (node && value !== undefined) node.textContent = String(value); }
  } catch (_) {}
}
refreshNativeDeviceSnapshot();

// Fleasion 0.7.0 — PC-style mobile modules. All data remains local.
const FLAGS_KEY = "fleasion-mobile-fflags-v1";
const ASSETS_KEY = "fleasion-mobile-assets-v1";
const UI_KEY = "fleasion-mobile-ui-v1";
let flagProfiles = {};
let assetEntries = [];
try { flagProfiles = JSON.parse(localStorage.getItem(FLAGS_KEY) || "{}") || {}; } catch { flagProfiles = {}; }
try { assetEntries = JSON.parse(localStorage.getItem(ASSETS_KEY) || "[]"); if (!Array.isArray(assetEntries)) assetEntries = []; } catch { assetEntries = []; }
function activeFlagKey() { return current().id; }
function activeFlags() { return flagProfiles[activeFlagKey()] || {}; }
function renderFlags() {
  const flags = activeFlags();
  $("#flagProfileName").textContent = current().name + " · FFlags";
  $("#flagCount").textContent = Object.keys(flags).length + " flag entries · local profile";
  $("#flagEditor").value = JSON.stringify(flags, null, 2);
}
function validateFlagObject(value) {
  if (!value || typeof value !== "object" || Array.isArray(value)) throw new Error("FFlags file must be a JSON object.");
  const entries = Object.entries(value);
  if (entries.length > 1000) throw new Error("Limit each profile to 1,000 entries.");
  for (const [key, val] of entries) {
    if (!key || key.length > 180) throw new Error("A flag name is empty or too long.");
    if (!["string","number","boolean"].includes(typeof val)) throw new Error("Flag values must be strings, numbers or booleans.");
    if (typeof val === "number" && !Number.isFinite(val)) throw new Error("Flag values must be finite numbers.");
  }
  return value;
}
$("#flagValidate").addEventListener("click", () => {
  try {
    const flags = validateFlagObject(JSON.parse($("#flagEditor").value || "{}"));
    $("#flagStatus").innerHTML = "<b>JSON valid · " + Object.keys(flags).length + " entries</b><p>Compatibility remains unverified. Valid JSON does not mean Roblox supports or applies these flags.</p>";
    toast("FFlags JSON is valid");
  } catch (error) {
    $("#flagStatus").innerHTML = "<b>Validation failed</b><p>" + escapeHtml(error.message) + "</p>";
    toast("FFlags validation failed");
  }
});
$("#flagSave").addEventListener("click", () => {
  try {
    flagProfiles[activeFlagKey()] = validateFlagObject(JSON.parse($("#flagEditor").value || "{}"));
    localStorage.setItem(FLAGS_KEY, JSON.stringify(flagProfiles));
    renderFlags();
    toast("FFlags profile saved locally");
  } catch (error) { toast("Cannot save: " + error.message); }
});
$("#flagExport").addEventListener("click", () => {
  try {
    const flags = validateFlagObject(JSON.parse($("#flagEditor").value || "{}"));
    downloadJson(flags, (current().name.toLowerCase().replace(/[^a-z0-9]+/g,"-") || "fleasion") + "-fflags.json");
    toast("FFlags JSON export prepared");
  } catch (error) { toast("Export failed: " + error.message); }
});
$("#flagImport").addEventListener("click", () => $("#flagFile").click());
$("#flagFile").addEventListener("change", async event => {
  const file = event.target.files && event.target.files[0];
  if (!file) return;
  try {
    if (file.size > 1024 * 1024) throw new Error("Choose a JSON file smaller than 1 MB.");
    const flags = validateFlagObject(JSON.parse(await file.text()));
    flagProfiles[activeFlagKey()] = flags;
    localStorage.setItem(FLAGS_KEY, JSON.stringify(flagProfiles));
    renderFlags();
    toast("PC FFlags JSON imported to this profile");
  } catch (error) { toast("Import failed: " + error.message); }
  finally { event.target.value = ""; }
});
function renderAssets() {
  $("#assetList").innerHTML = assetEntries.map((item, index) =>
    "<div class='item'><div class='itemcopy'><b>" + escapeHtml(item.name) + "</b><small>" + escapeHtml(item.note || "No notes") + (item.url ? " · " + escapeHtml(item.url) : "") + "</small></div><div class='itembuttons'><button class='danger' type='button' data-remove-asset='" + index + "'>Delete</button></div></div>"
  ).join("") || "<div class='empty'>No saved asset references yet.</div>";
}
$("#assetAdd").addEventListener("click", () => {
  const name = $("#assetId").value.trim();
  const url = $("#assetUrl").value.trim();
  const note = $("#assetNote").value.trim();
  if (!name) return toast("Enter an asset ID or name");
  if (url) {
    try { const parsed = new URL(url); if (parsed.protocol !== "https:") throw new Error(); }
    catch { return toast("Use a valid HTTPS reference URL"); }
  }
  assetEntries.unshift({ name: name.slice(0,100), url, note: note.slice(0,160), created: Date.now() });
  try { localStorage.setItem(ASSETS_KEY, JSON.stringify(assetEntries)); } catch { toast("Storage is full; entry is session-only"); }
  $("#assetId").value = ""; $("#assetUrl").value = ""; $("#assetNote").value = "";
  renderAssets(); toast("Asset reference saved");
});
$("#assetList").addEventListener("click", event => {
  const button = event.target.closest("[data-remove-asset]");
  if (!button) return;
  const index = Number(button.dataset.removeAsset);
  if (!Number.isInteger(index) || !assetEntries[index]) return;
  if (confirm("Delete this saved asset reference?")) {
    assetEntries.splice(index,1);
    try { localStorage.setItem(ASSETS_KEY, JSON.stringify(assetEntries)); } catch {}
    renderAssets(); toast("Asset reference deleted");
  }
});
$("#joinPlace").addEventListener("click", () => {
  const id = $("#placeId").value.trim();
  if (!/^[0-9]{1,24}$/.test(id)) return toast("Enter a numeric Place ID");
  const url = "https://www.roblox.com/games/" + id;
  window.location.href = url;
  $("#joinStatus").textContent = "Opened the public Roblox experience page for Place ID " + id + ".";
});
function applyUiSettings(settings) {
  document.body.classList.toggle("lite-ui", !!settings.lite);
  document.body.classList.toggle("compact-ui", !!settings.compact);
  document.body.classList.toggle("reduce-motion", !!settings.motion);
  $("#liteMode").checked = !!settings.lite;
  $("#compactMode").checked = !!settings.compact;
  $("#reduceMotion").checked = !!settings.motion;
  $("#fpsLiteToggle").checked = !!settings.lite;
}
let uiSettings = {};
try { uiSettings = JSON.parse(localStorage.getItem(UI_KEY) || "{}") || {}; } catch { uiSettings = {}; }
applyUiSettings(uiSettings);
$("#fpsLiteToggle").addEventListener("change", event => {
  uiSettings.lite = event.target.checked; applyUiSettings(uiSettings);
  try { localStorage.setItem(UI_KEY, JSON.stringify(uiSettings)); } catch {}
});
$("#applyUiPreset").addEventListener("click", () => {
  uiSettings = { lite: $("#liteMode").checked, compact: $("#compactMode").checked, motion: $("#reduceMotion").checked };
  applyUiSettings(uiSettings);
  try { localStorage.setItem(UI_KEY, JSON.stringify(uiSettings)); toast("Fleasion interface preset saved"); }
  catch { toast("Preset active for this session only"); }
});
const originalRender = render;
render = function() { originalRender(); renderFlags(); };
renderAssets();
renderFlags();

// Native Android bridge: report only verified Shizuku state; no system tweaks are run.
function refreshNativeShizuku() {
  const status = document.querySelector(".shizuku-status");
  if (!status) return;
  if (!window.FleasionNative) return;
  let state = "Unavailable";
  try { state = window.FleasionNative.getShizukuState(); } catch (_) {}
  const title = status.querySelector("b");
  const detail = status.querySelector("small");
  if (title) title.textContent = state;
  if (detail) detail.textContent = state === "Permission granted"
    ? "Shizuku is connected and permission is granted. No privileged changes have been made."
    : state === "Connected · permission needed"
      ? "Service detected. Grant permission only if you want to enable future supported features."
      : "Start Shizuku using its official setup guide. Standard workspace features work without it.";
}
const nativePermissionButton = document.querySelector("#nativeShizukuRequest");
if (nativePermissionButton) nativePermissionButton.addEventListener("click", () => {
  if (!window.FleasionNative) return toast("Native Shizuku bridge is unavailable");
  window.FleasionNative.requestShizukuPermission();
});
window.addEventListener("focus", refreshNativeShizuku);
window.addEventListener("focus", refreshNativeDeviceSnapshot);
document.addEventListener("visibilitychange", refreshNativeShizuku);
refreshNativeShizuku();
