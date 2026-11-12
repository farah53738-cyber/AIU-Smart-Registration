"use strict";
const $ = (id) => document.getElementById(id);
const esc = (v) =>
  String(v ?? "").replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
let me,
  terms = [],
  allSections = [],
  selected = [],
  version = 0,
  termId = 0,
  catalogPage = 0,
  reportPage = 0,
  csrf;
let manageCourses = [], manageTerms = [], managePeople = [], manageSections = [], currentRequests = [];
let busy = false,
  viewToken = 0,
  previewTimer;
const has = (...roles) => roles.some((r) => me?.roles?.includes(r));
function say(message, error = false) {
  $("message").hidden = !message;
  $("message").textContent = message;
  $("message").className = error ? "error" : "";
}
async function api(url, method = "GET", data) {
  if (method !== "GET" && !csrf) csrf = await api("/api/csrf");
  const res = await fetch(url, {
    method,
    headers: {
      Accept: "application/json",
      ...(data !== undefined ? { "Content-Type": "application/json" } : {}),
      ...(method !== "GET" ? { [csrf.headerName]: csrf.token } : {}),
    },
    body: data !== undefined ? JSON.stringify(data) : undefined,
  });
  if (res.status === 401 || (res.redirected && new URL(res.url).pathname === "/login")) {
    location.href = "/login";
    throw Error("Please sign in again.");
  }
  const result = await res.json().catch(() => ({}));
  if (!res.ok) {
    if (res.status === 403) throw Error(result.message || "You do not have permission to perform this action, or your session expired. Refresh and try again.");
    throw Error(result.message || result.detail || `Request failed (${res.status}). Please check your information and try again.`);
  }
  return cleanDisplayFields(result);
}
async function action(fn) {
  if (busy) return;
  busy = true;
  document.body.classList.add("loading");
  try {
    await fn();
  } catch (e) {
    say(e.message, true);
  } finally {
    busy = false;
    document.body.classList.remove("loading");
    if (routePending) { routePending = false; await route(); }
  }
}
function table(headers, rows) {
  return `<div class="tablewrap"><table><thead><tr>${headers.map((h) => `<th>${esc(h)}</th>`).join("")}</tr></thead><tbody>${rows.length ? rows.map((row) => `<tr>${row.map((c) => `<td>${c}</td>`).join("")}</tr>`).join("") : `<tr><td class="empty" colspan="${headers.length}">No records to show.</td></tr>`}</tbody></table></div>`;
}
const statusLabel = status => ({LEGACY_INVALID:"Archived",PENDING:"Pending review",APPROVED:"Approved",REJECTED:"Rejected",WITHDRAWN:"Withdrawn",ACTIVE:"Active",CANCELLED:"Cancelled",LEFT:"Left",FULFILLED:"Enrolled"}[status] || status);
const badge = (status) =>
  `<span class="badge ${esc(String(status).toLowerCase())}">${esc(statusLabel(status))}</span>`;
function displayLabel(value) {
  return String(value ?? "").replace(/\s*\(demo\)\s*/gi, " ").replace(/^demo\s+/i, "").trim();
}
function cleanDisplayFields(value) {
  if (Array.isArray(value)) return value.map(cleanDisplayFields);
  if (value && typeof value === "object") return Object.fromEntries(Object.entries(value).map(([k,v]) => [k, typeof v === "string" && ["name","displayName","student_name","instructor","term_name","term"].includes(k) ? displayLabel(v) : cleanDisplayFields(v)]));
  return value;
}
const classNumber = id => String(id ?? "").padStart(5, "0");
function parseClassNumber(value) {
  const text=String(value).trim();
  if(!/^\d+$/.test(text)||!Number.isSafeInteger(Number(text))||Number(text)<1) throw Error("Enter a valid numeric class number from Browse Courses.");
  return Number(text);
}
let registrationMatch=null;
const termOptions = () =>
  terms
    .map(
      (t) =>
    `<option value="${t.term_id}" ${Number(t.term_id) === termId ? "selected" : ""}>${esc(String(t.name).replace(/\s*\(Demo\)\s*$/i, ""))}</option>`,
    )
    .join("");
const termField = () =>
  `<label>Academic term<select id="term">${termOptions()}</select></label>`;
const btn = (label, act, id = "", secondary = false) =>
  `<button ${secondary ? 'class="secondary"' : ""} data-act="${act}" data-id="${id}">${esc(label)}</button>`;
// Authorization for client-side views is independent of whether a sidebar link is shown.
// The backend separately enforces authorization for every data operation.
function canView(page) {
  if (page === "inbox" || page === "home" || page === "catalog") return true;
  if (["plan", "register", "schedule", "readiness", "waitlists", "advisor"].includes(page)) return has("STUDENT");
  if (page === "requests") return has("STUDENT", "ADVISOR");
  if (["advisees", "advisorRegister"].includes(page)) return has("ADVISOR");
  if (page === "roster") return has("INSTRUCTOR", "REGISTRAR", "ADMIN");
  if (["reports", "manage", "courses", "sections", "notices"].includes(page)) return has("REGISTRAR", "ADMIN");
  if (["records", "enrollments", "terms", "prerequisites"].includes(page)) return has("REGISTRAR");
  if (page === "users") return has("ADMIN");
  return false;
}
const viewTitles = {advisorRegister:"Register a Student",inbox:"Notifications",home:"Overview",catalog:"Course catalog",plan:"Register for Classes",register:"Register for Classes",schedule:"My Schedule",readiness:"Registration Readiness",requests:"Registration requests",waitlists:"My waitlists",advisor:"My advisor",advisees:"My Students",roster:"Class rosters",reports:"Reports",manage:"Courses",courses:"Courses",sections:"Class Sections",records:"Student Records",enrollments:"Student Enrollments",terms:"Academic Terms",prerequisites:"Prerequisites",notices:"Publish Notice",users:"User management"};
function menu() {
  let links=[["home","Overview"]], supporting=[];
  if(has("STUDENT")) {links.push(["catalog","Browse Courses"],["register","Register for Classes"],["schedule","My Schedule"],["requests","My Requests"]);supporting.push(["waitlists","My Waitlists"],["advisor","My Advisor"]);}
  if(has("ADVISOR")) links.push(["requests","Review Requests"],["advisorRegister","Register a Student"],["advisees","My Students"],["catalog","Browse Courses"]);
  if(has("REGISTRAR","ADMIN")) links.push(["courses","Courses"],["sections","Class Sections"]);
  if(has("REGISTRAR")) links.push(["records","Student Records"]);
  if(has("INSTRUCTOR")) links.push(["roster","My Classes"],["catalog","Browse Courses"]);
  if(has("ADMIN")) links.push(["users","User Accounts"]);
  if(has("REGISTRAR")) supporting.push(["terms","Academic Terms"],["prerequisites","Prerequisites"]);
  if(has("REGISTRAR","ADMIN")) supporting.push(["roster","Class Rosters"],["notices","Publish Notice"]);
  const render=items=>[...new Map(items.map(x=>[x[0],x])).values()].map(([id,name])=>`<a href="#${id}" data-page="${id}">${name}</a>`).join("");
  $("nav").innerHTML=render(links)+(supporting.length?`<span class="nav-divider">OTHER SERVICES</span>${render(supporting)}`:"");
}
async function updateNotificationBell() {
  const count = $("notificationCount");
  if (!count) return;
  try {
    const notices = await api("/api/notifications");
    const unread = notices.filter(n => !n.read_at).length;
    count.textContent = unread > 99 ? "99+" : String(unread);
    count.hidden = unread === 0;
    $("notificationBell")?.setAttribute("aria-label", `${unread} unread notifications. Open inbox`);
  } catch { count.hidden = true; }
}
let routing = false, routePending = false;
async function route() {
  if (routing || busy) { routePending = true; return; }
  routing = true;
  try { await renderRoute(); }
  finally {
    routing = false;
    if (routePending) { routePending = false; await route(); }
  }
}
async function renderRoute() {
  if (has("STUDENT") && (!location.hash || location.hash === "#home")) {
    location.replace("/index.html");
    return;
  }

  clearTimeout(previewTimer);
  const token = ++viewToken;
  let page = location.hash.slice(1) || "home";
  if (has("STUDENT") && ["plan", "readiness"].includes(page)) { page = "register"; history.replaceState(null, "", "#register"); }
  if(page === "manage") { page="courses"; history.replaceState(null,"","#courses"); }
  staffSearch="";
  say("");
  updateNotificationBell();
  const activePage = page === "enrollments" ? "records" : page;
  document
    .querySelectorAll("nav a")
    .forEach((a) => { a.classList.toggle("active", a.dataset.page === activePage); if (a.dataset.page === activePage) a.setAttribute("aria-current", "page"); else a.removeAttribute("aria-current"); });
  $("title").textContent = viewTitles[page] || "AIU Course Registration System";
  $("notificationBell")?.classList.toggle("active", page === "inbox");
  $("content").innerHTML = "<p>Loading…</p>";
  try {
    if (!canView(page)) throw Error("This page is not available for your role.");
    const views = {advisorRegister,home,catalog,plan,register:plan,schedule,readiness,requests,waitlists,inbox,reports,manage,courses:manage,sections:manage,records:manage,enrollments:manage,terms:manage,prerequisites:manage,notices:manage,users,roster,advisor,advisees};
    if (typeof views[page] !== "function") throw Error("This page is unavailable.");
    await views[page]();
  } catch (e) {
    if (token === viewToken) {
      $("content").innerHTML =
        '<div class="card empty"><h2>This view could not be loaded.</h2><p>Check your connection, then try again.</p><button data-act="retryView">Try again</button></div>';
      say(e.message, true);
    }
  }
}
async function home() {
  const cards=has("ADVISOR")?[
    ["requests","Review Requests","Review pending registrations and make a decision."],
    ["advisorRegister","Register a Student","Add a class for one of your assigned students."],
    ["advisees","My Students","Find the students assigned to you."],
    ["catalog","Browse Courses","Look up courses, sections and class times."]
  ]:has("REGISTRAR","ADMIN")?[
    ["courses","Courses","Maintain course details and credit hours."],
    ["sections","Class Sections","Manage instructors, meeting times and capacity."],
    [has("REGISTRAR")?"records":"users",has("REGISTRAR")?"Student Records":"User Accounts",has("REGISTRAR")?"Review enrollments and completed courses.":"Manage accounts, roles and advisor assignments."],
  ]:[["roster","My Classes","View your assigned classes and student rosters."],["catalog","Browse Courses","Look up course and section information."]];
  let summary="";
  if(has("ADVISOR")) {const rows=await api("/api/requests");const pending=rows.filter(r=>r.status==="PENDING").length;summary=`<p class="dashboard-summary">${pending} registration request${pending===1?"":"s"} waiting for review.</p>`;}
  $("content").innerHTML=`<div class="page-intro"><span class="eyebrow">${esc(me.role.replaceAll("_"," "))} PORTAL</span><h2>Welcome, ${esc(me.displayName)}.</h2><p>Choose a task below.</p>${summary}</div><div class="grid staff-home">${cards.map(([id,title,desc])=>`<article class="card"><h3>${title}</h3><p>${desc}</p><a class="button secondary" href="#${id}">Open ${title.toLowerCase()} →</a></article>`).join("")}</div>`;
}
async function catalog() {
  $("content").innerHTML =
    `<section class="card"><div class="sectionhead"><h2>Find your next class</h2></div><form id="search"><div class="row">${termField()}<label>Course code or title<input name="q" placeholder="e.g. CSC340"></label><label>Instructor<input name="instructor" placeholder="Instructor name"></label><label>Day<select name="day"><option value="">Any day</option>${["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"].map((d, i) => `<option value="${i + 1}">${d}</option>`).join("")}</select></label></div><div class="row"><label>Starts after<input name="startsAfter" type="time"></label><label>Ends before<input name="endsBefore" type="time"></label><label><input type="checkbox" name="available">Available seats only</label><button>Search courses</button></div></form></section><section class="card"><div id="catalogRows"></div><div class="actions">${btn("Previous", "catalogPrev", "", true)}${btn("Next page", "catalogNext", "", true)}</div></section>`;
  $("search").onsubmit = (e) => {
    e.preventDefault();
    catalogPage = 0;
    action(searchCatalog);
  };
  $("term").onchange = () => {
    termId = Number($("term").value);
    sessionStorage.setItem("aiuTerm:" + me.username, String(termId));
    catalogPage = 0;
    action(searchCatalog);
  };
  await searchCatalog();
}
async function searchCatalog() {
  const data = new FormData($("search"));
  const p = new URLSearchParams({ termId, page: catalogPage });
  for (const [k, v] of data) if (v) p.set(k, k === "available" ? "true" : v);
  const rows = await api("/api/sections?" + p);
  const nextButton = document.querySelector('[data-act="catalogNext"]');
  if (nextButton) nextButton.disabled = rows.length < 50;
  const prevButton = document.querySelector('[data-act="catalogPrev"]');
  if (prevButton) prevButton.disabled = catalogPage === 0;
  $("catalogRows").innerHTML = table(
    ["Class number", "Course", "Section & instructor", "Meetings", "Seats", "Actions"],
    rows.map((s) => [
      `<strong>${esc(classNumber(s.section_id))}</strong>`,
      `${esc(s.code)}<p>${esc(s.title)} · ${s.credits} credits</p>`,
      `${esc(s.section_number)}<p>${esc(s.instructor)}</p>`,
      `${esc(s.schedule)}<p>${esc(s.room)}</p>`,
      Number(s.enrolled) >= Number(s.capacity) ? '<span class="badge pending">Full · waitlist available</span>' : `${Math.max(0, s.capacity - s.enrolled)} / ${s.capacity} available`,
      btn("View details", "details", s.section_id, true),
    ]),
  );
}
async function detail(id) {
  const s = await api("/api/sections/" + id);
  let eligibility = "";
  if (has("STUDENT")) {
    const e = await api("/api/prerequisites/check?sectionId=" + id).catch(
      () => null,
    );
    if (e)
      eligibility = `<p>Eligibility: ${e.eligible ? "Prerequisites satisfied." : esc("Missing: " + e.missingPrerequisites.join(", "))}</p>`;
  }
  $("detailBody").innerHTML =
    `<h2>${esc(s.code)} · ${esc(s.title)}</h2><p><strong>Class number: ${esc(classNumber(s.section_id))}</strong></p><p>${s.credits} credits · Section ${esc(s.section_number)} · ${esc(s.instructor)}</p><p>Room ${esc(s.room)} · ${Math.max(0, s.capacity - s.enrolled)} available seats</p><h3>Prerequisites</h3><p>${s.prerequisites.map((p) => esc(p.code)).join(", ") || "No prerequisites configured."}</p>${eligibility}${table(
      ["Day", "Start", "End", "Teaching dates"],
      s.meetings.map((m) => [
        esc(
          [
            "",
            "Monday",
            "Tuesday",
            "Wednesday",
            "Thursday",
            "Friday",
            "Saturday",
            "Sunday",
          ][m.day_of_week],
        ),
        esc(m.start_time),
        esc(m.end_time),
        esc(m.start_date) + " – " + esc(m.end_date),
      ]),
    )}`;
  $("details").showModal();
}
async function loadAllSections() {
  let rows = [],
    page = 0;
  while (page < 20) {
    const next = await api(`/api/sections?termId=${termId}&page=${page++}`);
    rows.push(...next);
    if (next.length < 50) break;
  }
  return rows;
}
async function plan() {
  const p = await api("/api/me/plan?termId=" + termId);
  selected = p.sectionIds.map(Number); version = p.version;
  allSections = await loadAllSections();
  registrationMatch=null;
  $("content").innerHTML = `<section class="card"><h2>Register by class number</h2><p>Find the class number in Browse Courses, then enter it here. Each section has its own number.</p><form id="classLookup"><div class="row">${termField()}<label>Class number<input id="classNumberInput" type="text" inputmode="numeric" pattern="[0-9]+" maxlength="16" required placeholder="e.g. 00007" autocomplete="off"></label><button type="submit">Find class</button></div></form><div id="registrationSections" aria-live="polite"><p class="muted">Enter a class number to see its details and seat availability.</p></div><a class="button secondary" href="#catalog">Browse course numbers</a></section>
  <section class="card"><div class="sectionhead"><h2>Selected classes</h2><span id="planSummary" class="badge"></span><span id="draftVersion" hidden></span></div><div id="draftRows"></div><div id="validation" aria-live="polite"></div><p class="hint">Your selections are saved automatically. Registration is confirmed only after advisor approval.</p><div class="actions"><button data-act="submitPlan" id="submitPlan" disabled>Submit registration</button></div><details class="schedule-preview"><summary>Preview weekly timetable</summary><p class="hint">Selected classes and confirmed enrollments are labeled separately.</p><div id="timetable"></div></details></section>`;
  $("classLookup").onsubmit=e=>{e.preventDefault();action(lookupClass);};
  $("classNumberInput").oninput=()=>{registrationMatch=null;$("registrationSections").innerHTML='<p class="muted">Select Find class to check this number.</p>';};
  $("term").onchange = () => { termId = Number($("term").value); sessionStorage.setItem("aiuTerm:" + me.username, String(termId)); action(plan); };
  await drawPlan();
}
function validateLookupSection(s) {
  if(Number(s.term_id)!==termId) throw Error("This class belongs to a different term. Change the academic term or enter another class number.");
  if(!(s.active===true||Number(s.active)===1)||!(s.course_active===true||Number(s.course_active)===1)) throw Error("This class is not available for registration.");
}
async function lookupClass() {
  const entered=$("classNumberInput").value;
  registrationMatch=null;
  $("registrationSections").innerHTML="";
  const id=parseClassNumber(entered);
  const section=await api("/api/sections/"+id);
  validateLookupSection(section);
  const wait=await api("/api/waitlists");
  if($("classNumberInput").value!==entered)return;
  registrationMatch={...section,onWaitlist:wait.some(w=>Number(w.section_id)===id&&w.status==="ACTIVE")};
  drawRegistrationSections();
}
function drawRegistrationSections() {
  const s=registrationMatch;
  if(!s||!$("registrationSections"))return;
  const full=Number(s.enrolled)>=Number(s.capacity);
  const button=selected.includes(Number(s.section_id))?'<span class="badge">Already selected</span>':s.onWaitlist?'<span class="badge">On your waitlist</span>':full?btn("Join waitlist","joinWaitlist",s.section_id):btn("Add class","addRegistration",s.section_id);
  $("registrationSections").innerHTML=`<article class="notice"><h3>${esc(s.code)} — ${esc(s.title)}</h3><p><strong>Class ${esc(classNumber(s.section_id))}</strong> · Section ${esc(s.section_number)} · ${esc(s.credits)} credits</p><p>${esc(s.schedule)} · ${esc(s.instructor)} · ${esc(s.room)}</p><p>${full?"This class is full. You can join its waitlist.":`${Math.max(0,s.capacity-s.enrolled)} seats available.`}</p><div class="actions">${button}${btn("View details","details",s.section_id,true)}</div>${s.onWaitlist?'<p><a href="#waitlists">View my waitlists →</a></p>':""}</article>`;
}
async function schedule() {
  const enrolled = (await api("/api/me/enrollments")).filter(s => Number(s.term_id) === termId);
  $("content").innerHTML = `<section class="card"><h2>My Schedule</h2><p>Only confirmed classes appear here. Requests awaiting approval are shown in My Requests.</p>${termField()}${table(["Course", "Section", "Class times", "Room"], enrolled.map(s => [esc(s.code)+" — "+esc(s.title),esc(s.section_number),esc(s.schedule),esc(s.room)]))}</section><section class="card"><h2>Weekly timetable</h2><div id="timetable"></div></section>`;
  $("term").onchange = () => {termId=Number($("term").value); sessionStorage.setItem("aiuTerm:"+me.username,String(termId));action(schedule);};
  await renderTimetable(enrolled.map(e => ({id:Number(e.section_id),kind:"enrolled"})));
}
async function drawPlan() {
  $("draftVersion").textContent = version;
  $("planSummary").textContent = `${selected.length} courses · ${selected.reduce((sum,id) => sum + Number(allSections.find(s => Number(s.section_id) === id)?.credits || 0), 0)} credits`;
  $("draftRows").innerHTML = table(
    ["Class number", "Course", "Section", "Schedule", ""],
    selected.map((id) => {
      const s = allSections.find((s) => Number(s.section_id) === id);
      return [
        esc(classNumber(id)),
        esc(s?.code || `Unavailable section ${id}`),
        esc(s?.section_number),
        esc(s?.schedule),
        btn("Remove", "removePlan", id, true),
      ];
    }),
  );
  $("submitPlan").disabled = true;
  const [p, r] = await Promise.all([
    api("/api/me/plan/validate", "POST", {
      termId,
      sectionIds: selected,
      version,
    }),
    api("/api/me/readiness?termId=" + termId),
  ]);

  const problems = selected.length ? [...new Set([...(p.errors || []), ...(r.errors || [])])] : [];

  $("validation").innerHTML = problems.length
    ? problems.map((e) => `<p class="badge invalid">${esc(e)}</p>`).join(" ")
    : selected.length ? '<p class="badge approved">Your selected classes are ready to submit.</p>' : '<p class="muted">No classes selected yet. Enter a class number above to get started.</p>';

  const canSubmit = selected.length > 0 && p.valid && r.ready;
  $("submitPlan").disabled = !canSubmit;
  $("submitPlan").title = canSubmit
    ? "Submit this plan to your advisor"
    : "Resolve the listed issues before submitting";
	
  const enrolled = (await api("/api/me/enrollments")).filter(
    (s) => Number(s.term_id) === termId,
  );
  const groups = [
    ...selected.map((id) => ({ id, kind: "draft" })),
    ...enrolled.map((e) => ({ id: Number(e.section_id), kind: "enrolled" })),
  ];
  await renderTimetable(groups);
}
async function renderTimetable(groups) {
  const meetings = await Promise.all(
    groups.map(async (g) => ({
      ...g,
      section: await api("/api/sections/" + g.id),
    })),
  );
  const days = [7, 1, 2, 3, 4, 5, 6];
  $("timetable").innerHTML = table(
    days.map(
      (d) =>
        [
          "",
          "Monday",
          "Tuesday",
          "Wednesday",
          "Thursday",
          "Friday",
          "Saturday",
          "Sunday",
        ][d],
    ),
    [
      days.map(
        (day) =>
          meetings
            .flatMap((g) =>
              g.section.meetings
                .filter((m) => Number(m.day_of_week) === day)
                .map((m) => ({ g, m })),
            )
            .sort((a, b) =>
              String(a.m.start_time).localeCompare(String(b.m.start_time)),
            )
            .map(
              ({ g, m }) =>
                `<div class="slot ${g.kind}"><small>${g.kind === "enrolled" ? "Enrolled" : "Selected"}</small><br><b>${esc(g.section.code)}</b><br>${esc(String(m.start_time).slice(0, 5))}–${esc(String(m.end_time).slice(0, 5))}<br>${esc(m.room)}<br><small>${esc(m.start_date)} – ${esc(m.end_date)}</small></div>`,
            )
            .join("") || '<span class="hint">—</span>',
      ),
    ],
  );
}
async function saveDraft() {
  const p = await api("/api/me/plan", "PUT", {
    termId,
    sectionIds: selected,
    version,
  });
  version = p.version;
  sessionStorage.removeItem("aiuSubmission");
}
async function addSection(id) {
  if (!id) throw Error("Choose a section.");
  if (selected.includes(id))
    throw Error("This section is already in your draft.");
  const before = [...selected];
  selected.push(id);
  try {
    await saveDraft();
  } catch (e) {
    selected = before;
    throw e;
  }
}

async function readiness() {
  const result = await api("/api/me/readiness?termId=" + termId);
  const check = (title, ok, message) =>
    `<article class="card"><h3>${title}</h3>${badge(ok ? "READY" : "ACTION NEEDED")}<p>${esc(message)}</p></article>`;
  $("content").innerHTML =
    `<section class="card"><h2>Registration Readiness</h2><p>Check your saved plan against current registration rules before submitting. This check does not submit a request or reserve seats.</p>${termField()}<p>${badge(result.ready ? "READY TO SUBMIT" : "REVIEW NEEDED")}</p><div class="actions"><a class="button" href="#plan">Edit my plan</a><button class="secondary" data-act="refreshReadiness">Check again</button></div></section>
    <div class="grid">${check("Registration window", result.registrationOpen, result.registrationOpen ? "Registration is open for this semester." : "Registration is closed. You can still save a draft for later.")}
    ${check("Academic advisor", result.advisorAssigned, result.advisorAssigned ? "An active advisor is assigned to you." : "Contact the registrar to assign an active advisor.")}
    ${check("Saved draft", result.draftCount > 0, result.draftCount + " section(s) in your saved plan.")}
    ${check("Pending requests", result.pendingRequests === 0, result.pendingRequests === 0 ? "No pending request for this semester." : "Review or withdraw your pending request before submitting another plan.")}</div>
    <section class="card" style="margin-top:20px"><h3>Prerequisites and seats</h3>${table(
      ["Course", "Schedule", "Prerequisites", "Seats available", "Section"],
      result.sections.map((s) => [
        esc(s.code),
        esc(s.schedule),
        s.missingPrerequisites.length
          ? esc("Missing: " + s.missingPrerequisites.join(", "))
          : badge("SATISFIED"),
        esc(s.availableSeats),
        badge(s.active ? "ACTIVE" : "INACTIVE"),
      ]),
    )}</section>
    <section class="card"><h3>Plan validation</h3>${result.errors.length ? result.errors.map((error) => `<p class="badge invalid">${esc(error)}</p>`).join("<br>") : '<p class="badge approved">Your saved plan passes the current registration checks.</p>'}<p class="hint">Prerequisites, seats and timetable conflicts are checked again when you submit and when your advisor approves. Academic holds and credit limits are not checked in this version. Confirm these requirements with the registrar.</p></section>`;
  $("term").onchange = () => {
    termId = Number($("term").value);
    sessionStorage.setItem("aiuTerm:" + me.username, String(termId));
    action(readiness);
  };
}
async function requests() {
  if(has("ADVISOR")) return advisorRequests();
  const rows = (await api("/api/requests")).filter(r => r.status !== "WITHDRAWN");
  currentRequests = rows;
  $("content").innerHTML =
    `<section class="card"><h2>${has("ADVISOR") ? "Review registration plans" : "My registration requests"}</h2><p>${has("ADVISOR") ? "Approval rechecks prerequisites, seats and conflicts, then enrolls the entire plan. A rejected request needs a reason." : "Track submitted registrations and advisor decisions. Pending requests can be withdrawn."}</p>${table(
      ["Term & submitted", ...(has("ADVISOR") ? ["Student"] : []), "Classes", "Status", "Actions"],
      rows.map((r) => [
        `<strong>${esc(terms.find(t => Number(t.term_id) === Number(r.term_id))?.name || "Registration request")}</strong><p>${esc(formatDate(r.requested_at))} · Ref. ${esc(r.request_id)}</p>`,
        ...(has("ADVISOR") ? [`${esc(r.student_name)}<p class="hint">University ID: ${esc(r.student_id)}</p>`] : []),
        r.items
          .map(
            (i) =>
              `<p><b>${esc(i.code)}</b> · Section ${esc(i.section_number)}<br>${esc(i.schedule)} ${btn("Details", "details", i.section_id, true)}</p>`,
          )
          .join(""),
        badge(r.status) + `<p>${esc(r.status === "LEGACY_INVALID" ? "This older request is inactive and does not confirm enrollment." : r.reason || "")}</p>`,
        r.status === "PENDING"
          ? has("ADVISOR")
            ? btn("Approve", "approve", r.request_id) +
              btn("Reject", "reject", r.request_id, true)
            : btn("Withdraw", "withdraw", r.request_id, true)
          : has("STUDENT") && ["REJECTED", "WITHDRAWN"].includes(r.status) ? btn("Select again", "copyRequest", r.request_id, true) : "—",
      ]),
    )}</section>`;
}
async function waitlists() {
  const [rows,sections]=await Promise.all([api("/api/waitlists"),loadAllSections()]);
  const active=new Set(rows.filter(w=>w.status==="ACTIVE").map(w=>Number(w.section_id)));
  const full=sections.filter(s=>Number(s.enrolled)>=Number(s.capacity));
  $("content").innerHTML=`<section class="card"><h2>My waitlists</h2><p>Joining a waitlist does not enroll you or reserve a seat. You will receive a notification if a seat becomes available.</p>${rows.length?table(["Class number","Course / section","Joined","Status",""],rows.map(w=>[esc(classNumber(w.section_id)),`${esc(w.code)} · ${esc(w.section_number)}`,esc(formatDate(w.joined_at)),badge(w.status),w.status==="ACTIVE"?btn("Leave waitlist","leaveWaitlist",w.waitlist_id,true):"—"])):'<p class="empty">You have not joined a waitlist yet. Full classes are listed below.</p>'}</section><section class="card"><h2>Full classes available to waitlist</h2>${termField()}${full.length?table(["Class number","Course / section","Class times",""],full.map(s=>[esc(classNumber(s.section_id)),`${esc(s.code)} — ${esc(s.title)}<p>Section ${esc(s.section_number)}</p>`,esc(s.schedule),active.has(Number(s.section_id))?'<span class="badge">On your waitlist</span>':btn("Join waitlist","joinWaitlist",s.section_id)])):'<p class="empty">No classes are full in this term. Waitlists become available when a section reaches its capacity.</p>'}<p class="hint">Prerequisites and registration dates are checked when joining.</p></section>`;
  $("term").onchange=()=>{termId=Number($("term").value);sessionStorage.setItem("aiuTerm:"+me.username,String(termId));action(waitlists);};
}
function formatDate(value) {
  if (!value) return "";
  const normalized = String(value).replace("T", " ");
  const parsed = new Date(String(value).replace(" ", "T"));
  return Number.isNaN(parsed.getTime()) ? normalized : parsed.toLocaleString(undefined, {year:"numeric",month:"short",day:"numeric",hour:"numeric",minute:"2-digit"});
}
async function inbox() {
  const rows = await api("/api/notifications");
  $("content").innerHTML =
    `<section class="card"><div class="sectionhead"><div><h2>Your updates</h2><p class="hint">Decisions, waitlists and university announcements in one place.</p></div><span class="badge">${rows.filter(n => !n.read_at).length} unread</span></div>${rows.length ? rows.map((n) => `<article class="notice ${n.read_at ? "is-read" : "is-unread"}"><span class="hint">${esc(n.type.replaceAll("_", " "))} · ${esc(formatDate(n.created_at))}</span><p>${esc(n.message)}</p>${n.read_at ? '<span class="hint">Read</span>' : btn("Mark as read", "read", n.notification_id, true)}</article>`).join("") : '<p class="empty">You’re all caught up. New updates will appear here.</p>'}</section>`;
}
async function advisor() {
  const rows = await api("/api/me/advisor");
  $("content").innerHTML =
    `<section class="card"><h2>Your academic advisor</h2>${rows.length ? rows.map((a) => `<h3>${esc(a.name)}</h3><p>${esc(a.university_id)}</p><p>Your advisor reviews submitted registration requests. Build a plan, check the timetable and submit it for review.</p>`).join("") : "<p>No advisor has been assigned. Contact the registrar.</p>"}</section>`;
}
let advisorStatus = "PENDING", advisorTerm = "", advisorSearch = "";
async function advisorRequests() {
  currentRequests=await api("/api/requests");
  $("content").innerHTML=`<section class="card"><h2>Registration requests</h2><p>Open a request to review the student's selected classes before making a decision.</p><div class="row"><label>Status<select id="advisorStatus">${[["PENDING","Pending review"],["APPROVED","Approved"],["REJECTED","Rejected"],["WITHDRAWN","Withdrawn"],["","All requests"]].map(([v,t])=>`<option value="${v}" ${v===advisorStatus?"selected":""}>${t}</option>`).join("")}</select></label><label>Academic term<select id="advisorTerm"><option value="">All terms</option>${terms.map(t=>`<option value="${t.term_id}" ${String(t.term_id)===advisorTerm?"selected":""}>${esc(t.name)}</option>`).join("")}</select></label><label>Student<input id="advisorSearch" type="search" value="${esc(advisorSearch)}" placeholder="Name or university ID"></label></div><div id="advisorRequestRows"></div></section>`;
  $("advisorStatus").onchange=()=>{advisorStatus=$("advisorStatus").value;drawAdvisorRequests();};
  $("advisorTerm").onchange=()=>{advisorTerm=$("advisorTerm").value;drawAdvisorRequests();};
  $("advisorSearch").oninput=()=>{advisorSearch=$("advisorSearch").value;drawAdvisorRequests();};
  drawAdvisorRequests();
}
function filteredAdvisorRequests() {
  const q=advisorSearch.trim().toLowerCase();
  return currentRequests.filter(r=>(!advisorStatus||r.status===advisorStatus)&&(!advisorTerm||String(r.term_id)===advisorTerm)&&(!q||`${r.student_name} ${r.student_id}`.toLowerCase().includes(q)));
}
function drawAdvisorRequests() {
  const rows=filteredAdvisorRequests();
  $("advisorRequestRows").innerHTML=rows.length?table(["Student","Term","Classes","Status",""],rows.map(r=>[`${esc(r.student_name)}<p>${esc(r.student_id)}</p>`,esc(terms.find(t=>Number(t.term_id)===Number(r.term_id))?.name||"—"),esc((r.items||[]).map(i=>i.code).join(", ")),badge(r.status),btn(r.status==="PENDING"?"Review":"View","reviewRequest",r.request_id,true)])):'<p class="empty">No requests match these filters.</p>';
}
function reviewRequest(id) {
  const r=currentRequests.find(r=>Number(r.request_id)===id);
  if(!r)throw Error("Request not found. Refresh the request list.");
  $("content").innerHTML=`<div class="actions"><button class="secondary" data-act="backRequests">← Back to requests</button></div><section class="card"><div class="sectionhead"><div><h2>${esc(r.student_name)}</h2><p>University ID: ${esc(r.student_id)} · ${esc(terms.find(t=>Number(t.term_id)===Number(r.term_id))?.name||"Term unavailable")}</p></div>${badge(r.status)}</div><p class="hint">Submitted ${esc(formatDate(r.requested_at))}${r.decided_at?` · Decision ${esc(formatDate(r.decided_at))}`:""}</p>${table(["Course","Section","Class times",""],(r.items||[]).map(i=>[`${esc(i.code)}<p>${esc(i.title)}</p>`,esc(i.section_number),esc(i.schedule),btn("Section details","details",i.section_id,true)]))}${r.status==="PENDING"?`<p class="hint">Approval rechecks prerequisites, available seats and timetable conflicts, then enrolls all classes together.</p><label>Reason for rejection<textarea id="decisionReason" maxlength="1000" placeholder="Required only when rejecting. Explain what the student needs to change."></textarea></label><div class="actions"><button data-act="approve" data-id="${r.request_id}">Approve registration</button><button class="danger" data-act="reject" data-id="${r.request_id}">Reject request</button></div>`:`<p>${esc(r.status==="LEGACY_INVALID"?"This older request is inactive and does not confirm enrollment.":r.reason||"No additional comments.")}</p>`}</section>`;
}
let advisorRegistration = null;
async function advisorRegister() {
  advisorRegistration = null;
  const students = await api("/api/advisees");
  $("content").innerHTML = `<section class="card"><h2>Register a student</h2><p>Select a student assigned to you, then enter a class number from <a href="#catalog">Browse Courses</a>.</p>${students.length ? `<form id="advisorRegisterForm"><div class="row"><label>Student<select id="registrationStudent" required><option value="">Choose a student</option>${students.map(s=>`<option value="${Number(s.user_id)}">${esc(s.name)} · ${esc(s.university_id)}</option>`).join("")}</select></label>${termField()}<label>Class number<input id="advisorClassNumber" inputmode="numeric" pattern="[0-9]+" required placeholder="e.g. 00001"></label></div><button type="submit">Review class</button></form><div id="advisorRegistrationReview" aria-live="polite"></div>` : '<p class="empty">No students are assigned to you.</p>'}</section>`;
  if (!students.length) return;
  const reset = () => {advisorRegistration=null;$("advisorRegistrationReview").innerHTML="";};
  $("registrationStudent").onchange=reset;
  $("term").onchange=reset;
  $("advisorClassNumber").oninput=reset;
  $("advisorRegisterForm").onsubmit=e=>{e.preventDefault();action(async()=>{
    reset(); say("");
    const data={studentId:Number($("registrationStudent").value),termId:Number($("term").value),sectionId:parseClassNumber($("advisorClassNumber").value)};
    const student=students.find(s=>Number(s.user_id)===data.studentId);
    if(!student) throw Error("Choose an assigned student.");
    const section=await api(`/api/sections/${data.sectionId}`);
    const check=await api("/api/advisor/registrations/validate","POST",data);
    const review=$("advisorRegistrationReview");
    review.innerHTML=`<h3>${esc(student.name)} · ${esc(student.university_id)}</h3><p><strong>${esc(section.code)} — ${esc(section.title)}</strong></p><p>Class ${esc(classNumber(section.section_id))} · Section ${esc(section.section_number)} · ${esc(terms.find(t=>Number(t.term_id)===data.termId)?.name||"")}</p><p>${esc(section.schedule)} · Room ${esc(section.room)}</p>${check.valid?'<p>This will enroll the student immediately and notify them.</p><button data-act="confirmAdvisorRegistration" type="button">Confirm registration</button>':`<div class="error">${(check.errors||[]).map(esc).join("<br>")}</div>`}`;
    if(check.valid) advisorRegistration={...data,idempotencyKey:crypto.randomUUID(),studentName:student.name};
  });};
}
async function confirmAdvisorRegistration() {
  const r=advisorRegistration;
  if(!r || Number($("registrationStudent").value)!==r.studentId || Number($("term").value)!==r.termId || parseClassNumber($("advisorClassNumber").value)!==r.sectionId)
    throw Error("Review the class again before confirming.");
  if(!confirm(`Register ${r.studentName} in class ${classNumber(r.sectionId)}?`)) return;
  const result=await api("/api/advisor/registrations","POST",{studentId:r.studentId,termId:r.termId,sectionId:r.sectionId,idempotencyKey:r.idempotencyKey});
  advisorRegistration=null;
  $("advisorRegistrationReview").innerHTML=`<p role="status">${esc(result.message)} The class is now on the student's schedule.</p><a href="#requests">View registration requests</a>`;
  say(result.message);
}

async function advisees() {
  adviseeRows=await api("/api/advisees");
  $("content").innerHTML=`<section class="card"><h2>My students</h2><p>Students assigned to you for academic advising.</p><label class="list-search">Find a student<input id="adviseeSearch" type="search" placeholder="Name or university ID"></label><div id="adviseeRows"></div></section>`;
  $("adviseeSearch").oninput=drawAdvisees;drawAdvisees();
}
function drawAdvisees() {
  const q=String($("adviseeSearch")?.value||"").toLowerCase();
  const rows=adviseeRows.filter(r=>`${r.name} ${r.university_id}`.toLowerCase().includes(q));
  $("adviseeRows").innerHTML=table(["Student","University ID",""],rows.map(r=>[esc(r.name),esc(r.university_id),`<button class="secondary" data-act="studentRequests" data-student="${esc(r.university_id)}">View requests</button>`]));
}

async function roster() {
  const rows = await api("/api/rosters");
  $("content").innerHTML = `<section class="card"><h2>Class rosters</h2>${table(
    ["Course", "Section", "Schedule", "Student ID", "Student"],
    rows.map((r) => [
      esc(r.code),
      esc(r.section_number),
      esc(r.schedule),
      esc(r.university_id || "—"),
      esc(r.name || "No active enrollment"),
    ]),
  )}</section>`;
}
async function reports() {
  $("content").innerHTML =
    `<section class="card"><h2>Registration reports</h2><div class="row">${termField()}<label>Report type<select id="reportType"><option value="seats">Seat availability</option><option value="enrollments">Enrollments</option><option value="requests">Registration requests</option><option value="waitlists">Waitlists</option></select></label><button data-act="loadReport">Generate report</button><button data-act="exportReport" class="secondary">Download this page (CSV)</button></div><p class="hint">Up to 100 rows per page. Results use current database records.</p><div id="reportRows"></div><div class="actions">${btn("Previous", "reportPrev", "", true)}${btn("Next page", "reportNext", "", true)}</div></section>`;
  $("term").onchange = () => {
    termId = Number($("term").value);
    sessionStorage.setItem("aiuTerm:" + me.username, String(termId));
    reportPage = 0;
    action(loadReport);
  };
  $("reportType").onchange = () => {
    reportPage = 0;
    action(loadReport);
  };
  await loadReport();
}
async function loadReport() {
  const rows = await api(
    `/api/reports/${$("reportType").value}?termId=${termId}&page=${reportPage}`,
  );
  const nextButton = document.querySelector('[data-act="reportNext"]');
  if (nextButton) nextButton.disabled = rows.length < 100;
  const prevButton = document.querySelector('[data-act="reportPrev"]');
  if (prevButton) prevButton.disabled = reportPage === 0;
  $("reportRows").innerHTML = rows.length
    ? table(
        Object.keys(rows[0]).map(columnLabel),
        rows.map((r) => Object.entries(r).map(([k,v]) => esc(k === "status" ? statusLabel(v) : v))),
      )
    : '<p class="empty">No records match this report.</p>';
}
function recordKeys(row) {
  return Object.keys(row).filter(k => !["course_id","term_id","instructor_id","student_id","prerequisite_id","request_id"].includes(k));
}
function columnLabel(key) {
  return String(key).replace(/_/g, " ").replace(/\bid\b/gi, "ID").replace(/^./, c => c.toUpperCase());
}
function meetingRow(m = {}) {
  const term = terms.find(t => Number(t.term_id) === Number($("sectionForm")?.elements.termId.value));
  return `<div class="meeting-row"><label>Day<select data-meeting="day">${["Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"].map((d,i) => `<option value="${i+1}" ${Number(m.day_of_week || 7) === i+1 ? "selected" : ""}>${d}</option>`).join("")}</select></label>
  <label>Start time<input data-meeting="start" type="time" required value="${esc(String(m.start_time || "09:00").slice(0,5))}"></label>
  <label>End time<input data-meeting="end" type="time" required value="${esc(String(m.end_time || "10:00").slice(0,5))}"></label>
  <label>First date<input data-meeting="startDate" type="date" required value="${esc(m.start_date || term?.start_date || "")}"></label>
  <label>Last date<input data-meeting="endDate" type="date" required value="${esc(m.end_date || term?.end_date || "")}"></label>
  <button type="button" class="secondary" data-act="removeMeeting">Remove meeting</button></div>`;
}
function renderMeetings(rows = [{}]) { $("meetingRows").innerHTML = (rows.length ? rows : [{}]).map(meetingRow).join(""); }
function readMeetings() {
  return [...document.querySelectorAll(".meeting-row")].map(row => {
    const m = Object.fromEntries([...row.querySelectorAll("[data-meeting]")].map(el => [el.dataset.meeting, el.value]));
    m.day = Number(m.day);
    if (!m.start || !m.end || m.end <= m.start) throw Error("Each meeting must end after it starts.");
    if (!m.startDate || !m.endDate || m.endDate < m.startDate) throw Error("Check the first and last date of each meeting.");
    return m;
  });
}
const hiddenId = () => `<input type="hidden" name="id" value="">`;
const field = (name, label, type = "text", value = "", required = true) =>
  `<label>${label}<input name="${name}" type="${type}" value="${esc(value)}" ${required ? "required" : ""}></label>`;
const opts = (name, label, rows, id, text, optional = false) =>
  `<label>${label}<select name="${name}">${optional ? '<option value="">New / none</option>' : ""}${rows.map((r) => `<option value="${r[id]}">${esc(text(r))}</option>`).join("")}</select></label>`;
const checkbox = (name, label, checked = true) =>
  `<label><input name="${name}" type="checkbox" ${checked ? "checked" : ""}>${label}</label>`;
const form = (id, title, body) =>
  `<form id="${id}" class="card manage-form"><details ${id === "courseForm" ? "open" : ""}><summary><span>${title}</span><small>Open form</small></summary><div class="manage-form-body">${body}<div class="actions"><button type="submit">Save changes</button>${["courseForm", "sectionForm", "termForm", "userForm"].includes(id) ? `<button type="button" class="secondary" data-act="newRecord" data-form="${id}">Create new instead</button>` : ""}</div></div></details></form>`;
const managementPages = new Set(["courses", "sections", "records", "enrollments", "terms", "prerequisites", "notices"]);
let completionIdentity = null;
let recordData = {}, staffSearch = "", staffTerm = "", rosterRows = [], adviseeRows = [];
function activeManagementPage() {
  const page = location.hash.slice(1);
  return managementPages.has(page) ? page : "courses";
}
const editor = (id, title, body) => `<form id="${id}" class="card manage-form"><details><summary><span>${title}</span></summary><div class="manage-form-body"><p id="${id}Mode" class="hint">New record</p>${body}<div class="actions"><button type="submit">Save</button><button type="button" class="secondary" data-act="cancelEditor" data-form="${id}">Cancel</button></div></div></details></form>`;
function staffTable(headers, rows) {
  return `<section class="card"><label class="list-search">Search this list<input id="staffSearch" type="search" placeholder="Type a name or course code" value="${esc(staffSearch)}"></label><div id="staffRows">${table(headers,rows)}</div></section>`;
}
function applyStaffSearch() {
  const q = String($("staffSearch")?.value || "").trim().toLowerCase(); staffSearch = q;
  let visible = 0;
  document.querySelectorAll("#staffRows tbody tr").forEach(row => {row.hidden = !!q && !row.textContent.toLowerCase().includes(q); if (!row.hidden) visible++;});
  let empty = $("staffNoMatches");
  if (!empty && $("staffRows")) {$("staffRows").insertAdjacentHTML("beforeend",'<p id="staffNoMatches" class="empty" hidden>No matching records. Try a different search.</p>');empty=$("staffNoMatches");}
  if (empty) empty.hidden = visible > 0;
}
async function manage() {
  completionIdentity = null;
  const page = activeManagementPage();
  if (!canView(page)) throw Error("This page is not available for your role.");
  const needsCourses = ["courses","sections","records","prerequisites"].includes(page);
  const needsPeople = ["sections","records"].includes(page);
  const needsRecords = ["sections","records","enrollments","prerequisites"].includes(page);
  const [courses,people,records] = await Promise.all([
    needsCourses ? api("/api/manage/courses") : [],
    needsPeople ? api("/api/manage/users") : [],
    needsRecords ? api("/api/manage/records") : {}
  ]);
  manageCourses=courses; manageTerms=terms; managePeople=people; manageSections=records.Sections||[]; recordData=records;
  const courseSelect = (name="courseId",label="Course") => opts(name,label,courses,"course_id",c=>`${c.code} — ${c.title}`);
  const termsSelect = () => opts("termId","Academic term",terms,"term_id",t=>t.name);
  const students = people.filter(p=>p.role==="STUDENT"), instructors=people.filter(p=>p.role==="INSTRUCTOR");
  const descriptions = {courses:"Maintain course titles, credit hours and availability.",sections:"Manage class sections, instructors, rooms and meeting times.",records:"Record completed courses and grades for students.",enrollments:"Review current enrollments and cancel a class when needed.",terms:"Set teaching dates and registration opening and closing times.",prerequisites:"Choose which courses must be completed before another course.",notices:"Send an academic notice to all active users."};
  let body="";
  if(page==="courses") {
    body=staffTable(["Course","Title","Credits","Status",""],courses.map(c=>[esc(c.code),esc(c.title),esc(c.credits),badge(c.active?"Active":"Inactive"),btn("Edit","editCourse",c.course_id,true)]))+
      editor("courseForm","Add or edit a course",hiddenId()+field("code","Course code")+field("title","Course title")+field("credits","Credits","number",3)+checkbox("active","Available"));
  } else if(page==="sections") {
    body=staffTable(["Class number","Course / section","Term","Instructor","Class times","Capacity",""],manageSections.map(s=>[esc(classNumber(s.section_id)),`${esc(s.code)} · ${esc(s.section_number)}<p>${esc(s.room)}</p>`,esc(s.term),esc(s.instructor),esc(s.schedule),esc(s.capacity),btn("Edit","editSection",s.section_id,true)]))+
      editor("sectionForm","Add or edit a section",hiddenId()+courseSelect()+termsSelect()+opts("instructorId","Instructor",instructors,"user_id",p=>p.name)+field("sectionNumber","Section number")+field("room","Room")+field("capacity","Capacity","number",30)+checkbox("active","Available")+`<fieldset class="meeting-editor"><legend>Class meetings</legend><p class="hint">Add each weekly meeting within the selected term.</p><div id="meetingRows"></div><button type="button" class="secondary" data-act="addMeeting">Add meeting</button></fieldset>`);
  } else if(page==="terms") {
    body=staffTable(["Term","Teaching dates","Registration opens","Registration closes",""],terms.map(t=>[esc(t.name),`${esc(t.start_date)} – ${esc(t.end_date)}`,esc(formatDate(t.registration_open)),esc(formatDate(t.registration_close)),btn("Edit","editTerm",t.term_id,true)]))+
      editor("termForm","Add or edit a term",hiddenId()+field("name","Term name")+field("start","First teaching date","date")+field("end","Last teaching date","date")+field("open","Registration opens","datetime-local")+field("close","Registration closes","datetime-local"));
  } else if(page==="records") {
    body=staffTable(["Student","Course","Term","Grade","Result",""],(records.Completions||[]).map(r=>[esc(r.student),esc(r.code),esc(r.term),esc(r.grade_label||"—"),badge(r.passed?"Passed":"Not passed"),btn("Edit","editCompletion",r.completion_id,true)]))+
      editor("completionForm","Add or update a completed course",opts("studentId","Student",students,"user_id",p=>`${p.name} · ${p.university_id}`)+courseSelect()+termsSelect()+field("grade","Grade","text","",false)+checkbox("passed","Passed"));
  } else if(page==="enrollments") {
    body=staffTable(["Student","Course / section","Term","Status",""],(records.Enrollments||[]).map(e=>[`${esc(e.student)}<p>${esc(e.university_id)}</p>`,`${esc(e.code)} · ${esc(e.section_number)}`,esc(e.term),badge(e.status),e.status==="ACTIVE"?btn("Cancel enrollment","cancelEnrollment",e.enrollment_id,true):"—"]));
  } else if(page==="prerequisites") {
    body=staffTable(["Course","Required course"],(records.Prerequisites||[]).map(r=>[esc(r.course),esc(r.prerequisite)]))+
      editor("prerequisiteForm","Add or remove a prerequisite",courseSelect()+courseSelect("prerequisiteId","Required course")+checkbox("remove","Remove this requirement",false));
  } else if(page==="notices") {
    body=`<form id="noticeForm" class="card simple-editor"><label>Notice<textarea name="message" maxlength="1000" required placeholder="Write a clear update for students and staff."></textarea></label><p class="hint">This message will appear in every active user's notifications.</p><div class="actions"><button type="submit">Publish notice</button></div></form>`;
  }
  const recordTabs=["records","enrollments"].includes(page)?`<nav class="page-tabs" aria-label="Student records"><a href="#records" ${page==="records"?'aria-current="page"':''}>Completed courses</a><a href="#enrollments" ${page==="enrollments"?'aria-current="page"':''}>Enrollments</a></nav>`:"";
  $("content").innerHTML=`<div class="page-intro"><h2>${esc(viewTitles[page])}</h2><p>${descriptions[page]}</p></div>${recordTabs}${body}${["records","enrollments"].includes(page)?'<p class="hint">This list shows up to 200 records. Use Reports for paginated enrollment information.</p>':""}`;
  if($("staffSearch")){ $("staffSearch").oninput=applyStaffSearch; applyStaffSearch(); }
  if(page==="sections") {
    renderMeetings();
    // Selecting a different term resets new meeting dates to that term; editing preserves dates until explicitly changed.
    $("sectionForm").elements.termId.onchange=()=>{if(!$("sectionForm").elements.id.value)renderMeetings();};
  }
  bindForm("courseForm","/api/manage/courses",d=>({...d,id:d.id?Number(d.id):null,credits:Number(d.credits),active:!!d.active}));
  bindForm("sectionForm","/api/manage/sections",d=>({...d,id:d.id?Number(d.id):null,courseId:Number(d.courseId),termId:Number(d.termId),instructorId:Number(d.instructorId),capacity:Number(d.capacity),active:!!d.active,meetings:readMeetings()}));
  bindForm("termForm","/api/manage/terms",d=>({...d,id:d.id?Number(d.id):null}));
  bindForm("completionForm","/api/manage/completions",d=>({...d,studentId:Number(d.studentId),courseId:Number(d.courseId),termId:Number(d.termId),passed:!!d.passed,...(completionIdentity||{})}));
  bindForm("prerequisiteForm","/api/manage/prerequisites",d=>({courseId:Number(d.courseId),prerequisiteId:Number(d.prerequisiteId),remove:!!d.remove}));
  bindForm("noticeForm","/api/notices",d=>d);
}

function bindForm(id, url, transform) {
  if (!$(id)) return;
  $(id).onsubmit = (e) => {
    e.preventDefault();
    action(async () => {
      if (id === "noticeForm" && !confirm("Publish this notice to all active users?")) return;
      const result = await api(
        url,
        "POST",
        transform(Object.fromEntries(new FormData(e.target))),
      );
      terms = await api("/api/terms");
      await (location.hash === "#users" ? users() : manage());
      say(result.message || "Your changes were saved successfully.");
    });
  };
}
async function users() {
  const people = await api("/api/manage/users");
  managePeople = people;
  $("content").innerHTML =
    `<div class="forms">` +
    form(
      "userForm",
      "User account",
      hiddenId() +
        field("username", "University ID") +
        field("name", "Display name") +
        `<label>Role<select name="role">${["STUDENT", "ADVISOR", "INSTRUCTOR", "REGISTRAR", "ADMIN"].map((r) => `<option>${r}</option>`).join("")}</select></label>` +
        field(
          "password",
          "Password (required for new accounts)",
          "password",
          "",
          false,
        ) +
        opts(
          "advisorId",
          "Advisor (students only)",
          people.filter((p) => p.role === "ADVISOR"),
          "user_id",
          (p) => p.name,
          true,
        ) +
        checkbox("active", "Active"),
    ) +
    `</div><section class="card"><h3>Users</h3>${table(
      ["ID", "University ID", "Name", "Role", "Active", "Edit"],
      people.map((p) => [
        p.user_id,
        esc(p.university_id),
        esc(p.name),
        esc(p.role),
        p.active ? "Yes" : "No",
        btn("Edit", "editUser", p.user_id, true),
      ]),
    )}</section>`;
  bindForm("userForm", "/api/manage/users", (d) => ({
    ...d,
    id: d.id ? Number(d.id) : null,
    advisorId: d.advisorId ? Number(d.advisorId) : null,
    active: !!d.active,
  }));
}
function populateForm(id, fields) {
  const form = $(id);
  if (!form) throw Error("The editing form is not available.");
  form.querySelector("details")?.setAttribute("open", "");
  if ($(id+"Mode")) $(id+"Mode").textContent = "Editing an existing record";
  for (const [name, value] of Object.entries(fields)) {
    const control = form.elements.namedItem(name);
    if (!control) continue;
    if (control.type === "checkbox") control.checked = Boolean(value);
    else control.value = value == null ? "" : String(value);
  }
  form.scrollIntoView({behavior:"smooth",block:"start"});
  form.querySelector("input:not([type=hidden]),select,textarea")?.focus({preventScroll:true});
}
$("content").addEventListener("click", (e) => {
  const b = e.target.closest("[data-act]");
  if (!b) return;
  const id = Number(b.dataset.id);
  action(async () => {
    switch (b.dataset.act) {
      case "confirmAdvisorRegistration": await confirmAdvisorRegistration(); break;
      case "reviewRequest": reviewRequest(id); break;
      case "backRequests": await advisorRequests(); break;
      case "studentRequests":
        advisorSearch=b.dataset.student; advisorStatus=""; advisorTerm="";
        if(location.hash === "#requests") await advisorRequests(); else location.hash="requests";
        break;
      case "cancelEditor": {
        const form=$(b.dataset.form);form.reset();
        if(b.dataset.form === "completionForm") {completionIdentity=null;for(const name of ["studentId","courseId","termId"])form.elements.namedItem(name).disabled=false;}
        const idField=form.elements.namedItem("id"); if(idField) idField.value="";
        form.querySelector("details")?.removeAttribute("open");
        if($(b.dataset.form+"Mode")) $(b.dataset.form+"Mode").textContent="New record";
        if(b.dataset.form === "sectionForm") renderMeetings();
        say(""); break;
      }
      case "editCompletion": {
        const r=(recordData.Completions||[]).find(r=>Number(r.completion_id)===id);
        if(!r)throw Error("Record not found. Refresh this page.");
        populateForm("completionForm",{studentId:r.student_id,courseId:r.course_id,termId:r.term_id,grade:r.grade_label,passed:r.passed});
        completionIdentity={studentId:Number(r.student_id),courseId:Number(r.course_id),termId:Number(r.term_id)};
        for(const name of ["studentId","courseId","termId"]) $("completionForm").elements.namedItem(name).disabled=true;
        say("Editing the grade and result for this completed course.");break;
      }
      case "cancelEnrollment": {
        const r=(recordData.Enrollments||[]).find(r=>Number(r.enrollment_id)===id);
        if(!r)throw Error("Enrollment not found. Refresh this page.");
        if(!confirm(`Cancel ${r.code} / ${r.section_number} for ${r.student}? This releases the seat.`))return;
        await api("/api/manage/enrollments/"+id+"/cancel","POST"); await manage();say("Enrollment cancelled. The seat is now available.");break;
      }
      case "retryView":
        routePending = true;
        break;
      case "addMeeting":
        if (document.querySelectorAll(".meeting-row").length >= 10) throw Error("A section can have up to 10 meetings.");
        $("meetingRows").insertAdjacentHTML("beforeend", meetingRow());
        break;
      case "removeMeeting":
        if (document.querySelectorAll(".meeting-row").length === 1) throw Error("Keep at least one meeting.");
        b.closest(".meeting-row").remove();
        break;
      case "newRecord": {
        const form = $(b.dataset.form);
        if (!form) throw Error("The form is not available.");
        form.reset();
        if (b.dataset.form === "sectionForm") renderMeetings();
        const idField = form.elements.namedItem("id");
        if (idField) idField.value = "";
        say("Ready to create a new record. Enter the details and save.");
        break;
      }
      case "editCourse": {
        const c = manageCourses.find(v => Number(v.course_id) === id);
        if (!c) throw Error("Course not found. Refresh the management page.");
        populateForm("courseForm", {id:c.course_id,code:c.code,title:c.title,credits:c.credits,active:c.active});
        say("Editing " + c.code + ". Review the details before saving.");
        break;
      }
      case "editSection": {
        const record = manageSections.find(v => Number(v.section_id) === id);
        if (!record) throw Error("Section not found. Refresh this page.");
        const detail = await api("/api/sections/" + id);
        populateForm("sectionForm", {
          id:detail.section_id,courseId:detail.course_id,termId:detail.term_id,
          instructorId:detail.instructor_id,sectionNumber:detail.section_number,
          room:detail.room,capacity:detail.capacity,active:detail.active,
          meetings:(detail.meetings || []).map(m => [m.day_of_week,String(m.start_time).slice(0,5),String(m.end_time).slice(0,5),m.start_date,m.end_date].join(",")).join("\n")
        });
        renderMeetings(detail.meetings);
        say("Editing " + detail.code + " · Section " + detail.section_number + ". Check meeting times before saving.");
        break;
      }
      case "editTerm": {
        const t = manageTerms.find(v => Number(v.term_id) === id);
        if (!t) throw Error("Semester not found. Refresh the management page.");
        const timeInput = v => String(v || "").replace(" ","T").slice(0,16);
        populateForm("termForm", {id:t.term_id,name:t.name,start:t.start_date,end:t.end_date,open:timeInput(t.registration_open),close:timeInput(t.registration_close)});
        say("Editing " + t.name + ". Review the registration dates before saving.");
        break;
      }
      case "editUser": {
        const u = managePeople.find(v => Number(v.user_id) === id);
        if (!u) throw Error("User not found. Refresh the users page.");
        populateForm("userForm", {id:u.user_id,username:u.university_id,name:u.name,role:u.role,active:u.active,advisorId:u.advisor_id,password:""});
        say("Editing " + u.name + ". Leave the password blank to keep the existing one. Check the advisor assignment before saving.");
        break;
      }
      case "refreshReadiness":
        await readiness();
        break;
      case "details":
        await detail(id);
        break;
      case "catalogPrev":
        catalogPage = Math.max(0, catalogPage - 1);
        await searchCatalog();
        break;
      case "catalogNext":
        if (document.querySelector("#catalogRows tbody tr.empty")) return;
        catalogPage++;
        await searchCatalog();
        break;
      case "catalogAdd": {
        const p = await api("/api/me/plan?termId=" + termId);
        selected = p.sectionIds.map(Number);
        version = p.version;
        await addSection(id);
        const validation = await api("/api/me/plan/validate", "POST", {termId, sectionIds:selected, version});
        say(validation.valid ? "Section saved. Open Review my plan to check your timetable and submit." : "Saved in your draft. " + (validation.errors || []).join(" "), !validation.valid);
        break;
      }
      case "addRegistration":
        if(!registrationMatch||Number(registrationMatch.section_id)!==id)throw Error("Enter the class number and select Find class first.");
        const latestClass=await api("/api/sections/"+id);validateLookupSection(latestClass);
        if(Number(latestClass.enrolled)>=Number(latestClass.capacity)){registrationMatch={...latestClass,onWaitlist:registrationMatch.onWaitlist};drawRegistrationSections();throw Error("This class just filled up. Join its waitlist instead.");}
        await addSection(id); await drawPlan(); drawRegistrationSections();
        say("Class added to your selection."); break;
      case "addPlan":
        await addSection(Number($("addSection").value));
        await drawPlan();
        say("Draft saved.");
        break;
      case "removePlan": {
        const old = [...selected];
        selected = selected.filter((x) => x !== id);
        try {
          await saveDraft();
        } catch (e) {
          selected = old;
          throw e;
        }
        await drawPlan(); drawRegistrationSections();
        break;
      }
      case "savePlan":
        await saveDraft();
        await drawPlan();
        say("Draft saved.");
        break;
      case "submitPlan": {
        if (!selected.length) throw Error("Add at least one class section before submitting.");
        if ($("submitPlan")?.disabled) throw Error("Resolve the readiness checks before submitting.");
        if (!confirm("Submit these selected classes to your academic advisor for approval?")) return;
        let key = sessionStorage.getItem("aiuSubmission");
        if (!key) {
          key = crypto.randomUUID();
          sessionStorage.setItem("aiuSubmission", key);
        }
        const result = await api("/api/requests", "POST", {
          termId,
          sectionIds: selected,
          planVersion: version,
          idempotencyKey: key,
        });
        sessionStorage.removeItem("aiuSubmission");
        history.replaceState(null, "", "#requests");
        await requests();
        $("title").textContent = viewTitles.requests;
        menu();
        document.querySelector('nav a[data-page="requests"]')?.classList.add("active");
        say(result.message || "Your registration request was submitted.");
        break;
      }
      case "copyRequest": {
        const request = currentRequests.find(r => Number(r.request_id) === id);
        if (!request) throw Error("Refresh the request list and try again.");
        if (!confirm("Replace your current class selection for this term with these classes?")) return;
        termId = Number(request.term_id);
        const draft = await api("/api/me/plan?termId=" + termId);
        selected = request.items.map(i => Number(i.section_id)); version = draft.version;
        await saveDraft(); location.hash = "register";
        break;
      }
      case "approve":
        if (!confirm("Approve this complete plan and enroll the student in all its sections?")) return;
        await api("/api/requests/" + id + "/status", "PATCH", {
          status: "APPROVED",
        });
        await requests();
        say("Registration approved. The student has been enrolled.");
        break;
      case "reject": {
        const reason = $("decisionReason")?.value || "";
        if (!reason.trim()) throw Error("Please enter a reason for rejecting this request.");
        if(!confirm("Reject this registration request and send the reason to the student?")) return;
        await api("/api/requests/" + id + "/status", "PATCH", {
          status: "REJECTED",
          reason,
        });
        await requests();
        say("Request rejected.");
        break;
      }
      case "withdraw":
        if (confirm("Withdraw this pending request?")) {
          await api("/api/requests/" + id + "/withdraw", "POST");
          await requests();
          say("Request withdrawn and removed from your request list.");
        }
        break;
      case "joinWaitlist":
        await api("/api/waitlists", "POST", { sectionId: id });
        if(location.hash === "#waitlists") await waitlists();
        else if(registrationMatch&&Number(registrationMatch.section_id)===id){registrationMatch.onWaitlist=true;drawRegistrationSections();}
        await updateNotificationBell();
        say("You joined the waitlist. Check My Waitlists and the notification bell for updates.");
        break;
      case "leaveWaitlist":
        await api("/api/waitlists/" + id, "DELETE");
        await waitlists();
        say("Waitlist left.");
        break;
      case "read":
        await api("/api/notifications/" + id + "/read", "PATCH");
        await inbox();
        await updateNotificationBell();
        break;
      case "exportReport": {
        const rows = [...document.querySelectorAll("#reportRows tr")];
        if (!rows.length) throw Error("There are no report rows to export.");
        const csv = rows.map(row => [...row.children].map(cell => {
          let value = cell.textContent.trim();
          if (/^[=+@-]/.test(value)) value = "'" + value;
          return '"' + value.replaceAll('"', '""') + '"';
        }).join(",")).join("\r\n");
        const url = URL.createObjectURL(new Blob(["\ufeff" + csv], {type:"text/csv;charset=utf-8"}));
        const link = document.createElement("a"); link.href = url;
        link.download = `registration-${$("reportType").value}-page-${reportPage+1}.csv`;
        link.click(); setTimeout(() => URL.revokeObjectURL(url), 1000);
        break;
      }
      case "loadReport":
        reportPage = 0;
        await loadReport();
        break;
      case "reportPrev":
        reportPage = Math.max(0, reportPage - 1);
        await loadReport();
        break;
      case "reportNext":
        if (document.querySelector("#reportRows .empty")) return;
        reportPage++;
        await loadReport();
        break;
    }
  });
});
$("closeDialog").onclick = () => $("details").close();
$("logout").onclick = () =>
  action(async () => {
    const token = await api("/api/csrf");
    const form = document.createElement("form");
    form.method = "POST";
    form.action = "/logout";
    const field = document.createElement("input");
    field.type = "hidden";
    field.name = token.parameterName;
    field.value = token.token;
    form.append(field);
    document.body.append(form);
    form.submit();
  });
window.addEventListener("hashchange", () => {
  route();
});
(async () => {
  try {
    me = await api("/api/me");
    $("account").textContent = me.displayName + " · " + me.role;
    terms = await api("/api/terms");
    termId = Number(
      terms.find((t) => t.is_open === true || Number(t.is_open) === 1)
        ?.term_id ||
        terms[0]?.term_id ||
        0,
    );
    const savedTerm = Number(sessionStorage.getItem("aiuTerm:" + me.username));
    if (terms.some(t => Number(t.term_id) === savedTerm)) termId = savedTerm;
    menu();
    await route();
    await updateNotificationBell();
  } catch (e) {
    say(e.message, true);
  }
})();
