const root = document.getElementById("app");
const nav = document.getElementById("navigation");
const toast = document.getElementById("toast");
const TOKEN_KEY = "gather_access_token";
const USER_KEY = "gather_current_user";

let toastTimer;
let authMode = "login";

const token = () => sessionStorage.getItem(TOKEN_KEY);
const user = () => {
  try {
    return JSON.parse(sessionStorage.getItem(USER_KEY) || "null");
  } catch {
    return null;
  }
};

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (character) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  })[character]);
}

function notify(message, isError = false) {
  toast.textContent = message;
  toast.classList.toggle("error", isError);
  toast.classList.add("visible");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove("visible"), 3200);
}

async function api(path, options = {}) {
  const headers = new Headers(options.headers || {});
  if (options.body !== undefined) headers.set("Content-Type", "application/json");
  if (token() && !path.startsWith("/api/auth/") && !path.startsWith("/api/public/")) {
    headers.set("Authorization", `Bearer ${token()}`);
  }
  let response;
  try {
    response = await fetch(path, { ...options, headers });
  } catch {
    throw new Error("Can't reach the server. Check that the app is running and try again.");
  }
  if (response.status === 401 && !path.startsWith("/api/auth/") && !path.startsWith("/api/public/")) {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(USER_KEY);
    render();
    throw new Error("Your session expired. Please sign in again.");
  }
  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const body = await response.json();
      message = body.error || body.message || message;
    } catch {
      // An empty response body is expected for some HTTP errors.
    }
    throw new Error(message);
  }
  return response.status === 204 ? null : response.json();
}

function navigate(path) {
  history.pushState({}, "", path);
  render();
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function setLoading(message = "Just a moment…") {
  root.innerHTML = `<div class="loading">${escapeHtml(message)}</div>`;
}

function updateNavigation() {
  const current = user();
  if (current) {
    nav.innerHTML = `
      <button class="button quiet" data-action="dashboard">My surveys</button>
      ${current.role === "ADMIN" ? '<button class="button quiet" data-action="admin">Admin</button>' : ""}
      <span class="small-text muted">${escapeHtml(current.name)}</span>
      <button class="button secondary small" data-action="logout">Log out</button>`;
  } else if (location.pathname.startsWith("/survey/")) {
    nav.innerHTML = '<a class="button secondary small" href="/">Gather home</a>';
  } else {
    nav.innerHTML = `
      <button class="button quiet" data-action="login">Log in</button>
      <button class="button small" data-action="register">Get started</button>`;
  }
}

async function render() {
  updateNavigation();
  const publicMatch = location.pathname.match(/^\/survey\/([a-f0-9-]+)$/i);
  if (publicMatch) {
    await renderPublicSurvey(publicMatch[1]);
    return;
  }
  if (token() && user()) {
    const builderMatch = location.pathname.match(/^\/builder(?:\/(\d+))?$/);
    const analyticsMatch = location.pathname.match(/^\/analytics\/(\d+)$/);
    if (builderMatch) return renderBuilder(builderMatch[1] ? Number(builderMatch[1]) : null);
    if (analyticsMatch) return renderAnalytics(Number(analyticsMatch[1]));
    if (location.pathname === "/admin" && user().role === "ADMIN") return renderAdmin();
    if (location.pathname !== "/" && location.pathname !== "/dashboard") navigate("/dashboard");
    return renderDashboard();
  }
  if (location.pathname !== "/" && !location.pathname.startsWith("/survey/")) {
    history.replaceState({}, "", "/");
  }
  renderLanding();
}

function renderLanding() {
  root.innerHTML = `
    <section class="hero">
      <div class="hero-copy">
        <span class="eyebrow">A calmer way to ask</span>
        <h1>Good questions.<br>Better decisions.</h1>
        <p>Create thoughtful surveys, collect responses in one place, and turn what you learn into your next great idea.</p>
        <div class="hero-actions">
          <button class="button" data-action="register">Create your first survey <span aria-hidden="true">→</span></button>
          <button class="button secondary" data-action="login">I already have an account</button>
        </div>
        <div class="hero-note"><span class="avatars"><i class="avatar">A</i><i class="avatar">M</i><i class="avatar">J</i><i class="avatar">S</i></span><span>Made for teams that want to listen.</span></div>
      </div>
      <div class="hero-visual" aria-label="Survey preview illustration">
        <div class="shape"></div>
        <div class="survey-preview">
          <div class="preview-head"><span class="preview-label">A quick check-in</span><span class="preview-dots"><i></i><i></i><i></i></span></div>
          <div class="preview-title">How was your week?</div>
          <div class="preview-description">Your honest feedback helps us make next week even better.</div>
          <div class="preview-question">How are you feeling about the project?</div>
          <div class="preview-option active"><span class="preview-radio"></span>Really good</div>
          <div class="preview-option"><span class="preview-radio"></span>Getting there</div>
          <div class="preview-option"><span class="preview-radio"></span>Could use a hand</div>
          <button class="preview-submit" tabindex="-1">Send my response</button>
        </div>
        <div class="floating-note"><strong>4.8 <span style="color:#e5a35b">★</span></strong>average happiness</div>
      </div>
    </section>
    <div class="section-title">Everything you need to go from a blank page to real insight.</div>
    <section class="feature-strip">
      <article class="feature"><span class="feature-icon">✳</span><div><h3>Build it your way</h3><p>Mix text, choice, yes/no, and rating questions in a simple, flexible builder.</p></div></article>
      <article class="feature"><span class="feature-icon">↗</span><div><h3>Share in a heartbeat</h3><p>Publish a link anyone can open. No account or app needed for respondents.</p></div></article>
      <article class="feature"><span class="feature-icon">⌁</span><div><h3>See what matters</h3><p>Watch responses come in and explore clear, question-by-question results.</p></div></article>
    </section>`;
}

function renderAuth(mode) {
  authMode = mode;
  const registering = mode === "register";
  root.innerHTML = `
    <section class="auth-wrap">
      <div class="auth-aside">
        <span class="eyebrow">${registering ? "Your next good idea starts here" : "Welcome back"}</span>
        <h1>${registering ? "Make room for better answers." : "Your questions are right where you left them."}</h1>
        <p>${registering ? "Create an account and build a survey in a few simple steps. Your response dashboard will be ready when you are." : "Sign in to pick up where you left off and see what your respondents have shared."}</p>
      </div>
      <form class="auth-card" id="auth-form">
        <h2>${registering ? "Create your account" : "Sign in"}</h2>
        <p>${registering ? "Start creating your first survey today." : "Enter your details to continue."}</p>
        ${registering ? '<div class="form-field"><label for="auth-name">Your name</label><input id="auth-name" name="name" autocomplete="name" maxlength="100" required placeholder="Jamie Taylor"></div>' : ""}
        <div class="form-field"><label for="auth-email">Email address</label><input id="auth-email" name="email" type="email" autocomplete="email" maxlength="255" required placeholder="you@example.com"></div>
        <div class="form-field"><label for="auth-password">Password</label><input id="auth-password" name="password" type="password" autocomplete="${registering ? "new-password" : "current-password"}" minlength="${registering ? "8" : "1"}" maxlength="72" required placeholder="${registering ? "At least 8 characters" : "Your password"}"></div>
        ${registering ? '<div class="form-field"><label for="auth-admin-token">Admin setup code <span class="muted">(optional)</span></label><input id="auth-admin-token" name="adminBootstrapToken" type="password" autocomplete="off" placeholder="Only provided during admin setup"></div>' : ""}
        <p id="auth-error" class="form-error" role="alert"></p>
        <button class="button full-width" type="submit">${registering ? "Create account" : "Sign in"} <span aria-hidden="true">→</span></button>
        ${!registering ? '<p class="auth-switch"><button class="text-button" type="button" data-action="forgot-password">Forgot your password?</button></p>' : ""}
        <p class="auth-switch">${registering ? "Already have an account?" : "New to Gather?"}
          <button class="text-button" type="button" data-action="${registering ? "login" : "register"}">${registering ? "Sign in" : "Create an account"}</button>
        </p>
      </form>
    </section>`;
}

function renderForgotPassword() {
  root.innerHTML = `
    <section class="auth-wrap"><div class="auth-aside"><span class="eyebrow">Account recovery</span>
      <h1>Let’s get you back in.</h1><p>We’ll email a short-lived reset code if an account matches your email.</p></div>
      <form class="auth-card" id="forgot-form"><h2>Reset your password</h2>
        <p>Enter the email address on your account.</p>
        <div class="form-field"><label for="forgot-email">Email address</label><input id="forgot-email" name="email" type="email" autocomplete="email" maxlength="255" required placeholder="you@example.com"></div>
        <p id="forgot-error" class="form-error" role="alert"></p>
        <button class="button full-width" type="submit">Send reset code <span aria-hidden="true">→</span></button>
        <p class="auth-switch"><button class="text-button" type="button" data-action="login">Back to sign in</button></p>
      </form></section>`;
}

function renderResetPassword(email) {
  root.innerHTML = `
    <section class="auth-wrap"><div class="auth-aside"><span class="eyebrow">Account recovery</span>
      <h1>Choose a new password.</h1><p>Your reset code is valid for 10 minutes and can only be used once.</p></div>
      <form class="auth-card" id="reset-form"><h2>Set a new password</h2>
        <div class="form-field"><label for="reset-email">Email address</label><input id="reset-email" name="email" type="email" autocomplete="email" maxlength="255" required value="${escapeHtml(email)}"></div>
        <div class="form-field"><label for="reset-code">6-digit reset code</label><input id="reset-code" name="code" inputmode="numeric" autocomplete="one-time-code" pattern="[0-9]{6}" minlength="6" maxlength="6" required></div>
        <div class="form-field"><label for="reset-password">New password</label><input id="reset-password" name="password" type="password" autocomplete="new-password" minlength="8" maxlength="72" required placeholder="At least 8 characters"></div>
        <p id="reset-error" class="form-error" role="alert"></p>
        <button class="button full-width" type="submit">Update password <span aria-hidden="true">→</span></button>
        <p class="auth-switch"><button class="text-button" type="button" data-action="login">Back to sign in</button></p>
      </form></section>`;
}

async function renderDashboard() {
  setLoading("Getting your surveys…");
  try {
    const surveys = await api("/api/surveys");
    const totalResponses = surveys.reduce((sum, survey) => sum + survey.responseCount, 0);
    const published = surveys.filter((survey) => survey.status === "PUBLISHED").length;
    root.innerHTML = `
      <section class="page-heading">
        <div><span class="eyebrow">Your workspace</span><h1>Good to see you, ${escapeHtml(user().name.split(/\s+/)[0])}.</h1><p>Here’s what’s happening with your surveys.</p></div>
        <button class="button" data-action="new-survey"><span aria-hidden="true">＋</span> New survey</button>
      </section>
      <section class="stat-grid" aria-label="Survey summary">
        <div class="stat-card"><div class="stat-label">All surveys</div><div class="stat-value">${surveys.length}</div></div>
        <div class="stat-card"><div class="stat-label">Published</div><div class="stat-value">${published}</div></div>
        <div class="stat-card"><div class="stat-label">Responses collected</div><div class="stat-value">${totalResponses}</div></div>
        <div class="stat-card"><div class="stat-label">Listening is a superpower</div><div class="stat-value" style="color:var(--green)">Keep going <span style="font-size:17px">✳</span></div></div>
      </section>
      <section>
        <div class="section-head"><h2>Your surveys</h2><span class="small-text muted">${surveys.length} total</span></div>
        <div class="survey-grid">${surveys.length ? surveys.map(surveyCard).join("") : `
          <div class="empty-state"><div class="empty-icon">✳</div><h3>Your first survey is waiting</h3><p>Start with a question you’ve been curious about. You can always edit it before anyone sees it.</p><button class="button small" data-action="new-survey">Create a survey</button></div>`}</div>
      </section>`;
  } catch (error) {
    root.innerHTML = `<div class="empty-state"><h3>We couldn't load your surveys</h3><p>${escapeHtml(error.message)}</p><button class="button small" data-action="dashboard">Try again</button></div>`;
  }
}

function surveyCard(survey) {
  const status = escapeHtml(survey.status);
  const shareAction = survey.status === "PUBLISHED"
    ? `<button class="icon-button" title="Copy public link" aria-label="Copy public link" data-action="copy-link" data-token="${escapeHtml(survey.publicToken)}">↗ Share</button>`
    : survey.status === "DRAFT"
      ? `<button class="icon-button" title="Publish survey" data-action="publish" data-id="${survey.id}">Publish</button>`
      : "";
  return `
    <article class="survey-card">
      <div class="card-top"><h3 title="${escapeHtml(survey.title)}">${escapeHtml(survey.title)}</h3><span class="status ${status}">${status}</span></div>
      <p>${escapeHtml(survey.description || "A survey ready to become something useful.")}</p>
      <div class="card-meta"><span>${survey.responseCount} ${survey.responseCount === 1 ? "response" : "responses"}</span>
        <div class="card-actions">
          ${shareAction}
          <button class="icon-button" title="View insights" aria-label="View insights" data-action="analytics" data-id="${survey.id}">Insights</button>
          <button class="icon-button" title="Edit survey" aria-label="Edit survey" data-action="edit-survey" data-id="${survey.id}">Edit</button>
          ${survey.status === "PUBLISHED" ? `<button class="icon-button" title="Close survey" aria-label="Close survey" data-action="close-survey" data-id="${survey.id}">Close</button>` : ""}
          <button class="icon-button" title="Delete survey" aria-label="Delete survey" data-action="delete-survey" data-id="${survey.id}">×</button>
        </div>
      </div>
    </article>`;
}

async function renderBuilder(surveyId) {
  setLoading(surveyId ? "Opening your survey…" : "Getting your survey builder ready…");
  try {
    const survey = surveyId ? await api(`/api/surveys/${surveyId}`) : {
      title: "", description: "", status: "DRAFT", questions: [],
    };
    if (survey.status === "PUBLISHED") {
      notify("Close this survey before making changes.", true);
      navigate("/dashboard");
      return;
    }
    const questions = survey.questions.map((question) => ({
      id: question.id,
      questionText: question.questionText,
      questionType: question.questionType,
      required: question.required,
      options: question.options,
    }));
    root.innerHTML = `
      <section class="page-heading">
        <div><button class="text-button" data-action="dashboard">← All surveys</button><h1 style="margin-top:8px">${surveyId ? "Edit your survey" : "Create a survey"}</h1><p>A clear title and a few thoughtful questions go a long way.</p></div>
      </section>
      <div class="builder-layout">
        <form id="builder-form">
          <section class="builder-card">
            <div class="builder-title">Start with the basics</div>
            <div class="form-field"><label for="survey-title">Survey title</label><input id="survey-title" name="title" maxlength="160" required value="${escapeHtml(survey.title)}" placeholder="e.g. A little feedback about our workshop"></div>
            <div class="form-field"><label for="survey-description">A short introduction <span class="muted">(optional)</span></label><textarea id="survey-description" name="description" maxlength="2000" placeholder="Tell people why their answers matter…">${escapeHtml(survey.description)}</textarea></div>
          </section>
          <section id="question-list">
            ${questions.map((question, index) => questionEditor(question, index)).join("")}
          </section>
          <button class="button secondary" type="button" data-action="add-question">＋ Add a question</button>
        </form>
        <aside class="builder-sidebar">
          <div class="panel"><span class="eyebrow">A little guide</span><h3>Make it easy to answer</h3><p>Keep questions clear and focused. You can save a draft and return to it anytime.</p>
            <div class="builder-actions">
              <button class="button" data-action="save-survey" data-id="${surveyId || ""}">${surveyId ? "Save changes" : "Save draft"}</button>
              <button class="button secondary" data-action="save-publish" data-id="${surveyId || ""}">Save & publish</button>
            </div>
          </div>
        </aside>
      </div>`;
  } catch (error) {
    notify(error.message, true);
    navigate("/dashboard");
  }
}

function questionEditor(question, index) {
  const type = question.questionType || "TEXT";
  const optionsVisible = type === "SINGLE_CHOICE" || type === "MULTIPLE_CHOICE";
  return `
    <section class="question-card" data-question data-question-id="${question.id || ""}">
      <div class="question-toolbar"><span class="question-number">Question ${index + 1}</span><button class="icon-button" type="button" data-action="remove-question" title="Remove question">Remove ×</button></div>
      <div class="question-fields">
        <div class="form-field" style="margin:0"><label>Question text</label><input data-question-text maxlength="1000" required value="${escapeHtml(question.questionText || "")}" placeholder="What would you like to know?"></div>
        <div class="form-field" style="margin:0"><label>Answer type</label><select data-question-type>
          ${[
            ["TEXT", "Short text"], ["LONG_TEXT", "Long text"], ["SINGLE_CHOICE", "Single choice"],
            ["MULTIPLE_CHOICE", "Multiple choice"], ["RATING", "1–5 rating"], ["YES_NO", "Yes / no"],
          ].map(([value, label]) => `<option value="${value}" ${type === value ? "selected" : ""}>${label}</option>`).join("")}
        </select></div>
      </div>
      <div class="options-field" ${optionsVisible ? "" : "hidden"}>
        <div class="form-field" style="margin:0"><label>Answer choices <span class="muted">(one per line)</span></label><textarea data-question-options placeholder="First option&#10;Second option">${escapeHtml((question.options || []).filter((option) => !(type === "YES_NO" && ["Yes", "No"].includes(option))).join("\n"))}</textarea><p class="options-hint">Add between 2 and 20 unique choices.</p></div>
      </div>
      <div class="question-footer"><label class="required-toggle"><input data-question-required type="checkbox" ${question.required ? "checked" : ""}> Required question</label><span class="small-text muted">${optionsVisible ? "Choice question" : type === "YES_NO" ? "Two fixed answer choices" : " "}</span></div>
    </section>`;
}

async function saveBuilder(publishAfter) {
  const form = document.getElementById("builder-form");
  if (!form.reportValidity()) return;
  const originalId = new URLSearchParams(location.search).get("id");
  const existingRoute = location.pathname.match(/^\/builder\/(\d+)$/);
  const surveyId = existingRoute ? Number(existingRoute[1]) : originalId ? Number(originalId) : null;
  const request = {
    title: form.elements.title.value.trim(),
    description: form.elements.description.value.trim(),
  };
  const cards = [...form.querySelectorAll("[data-question]")];
  const questions = cards.map((card) => ({
    id: card.dataset.questionId ? Number(card.dataset.questionId) : null,
    questionText: card.querySelector("[data-question-text]").value.trim(),
    questionType: card.querySelector("[data-question-type]").value,
    required: card.querySelector("[data-question-required]").checked,
    options: card.querySelector("[data-question-options]").value.split("\n").map((option) => option.trim()).filter(Boolean),
  }));
  for (const question of questions) {
    if (["SINGLE_CHOICE", "MULTIPLE_CHOICE"].includes(question.questionType)
      && (question.options.length < 2 || question.options.length > 20)) {
      notify("Each choice question needs between 2 and 20 options.", true);
      return;
    }
  }
  const saveButton = document.querySelector('[data-action="save-survey"]');
  const publishButton = document.querySelector('[data-action="save-publish"]');
  if (saveButton) saveButton.disabled = true;
  if (publishButton) publishButton.disabled = true;
  try {
    let saved;
    if (surveyId) {
      saved = await api(`/api/surveys/${surveyId}`, { method: "PUT", body: JSON.stringify(request) });
    } else {
      saved = await api("/api/surveys", { method: "POST", body: JSON.stringify(request) });
    }
    const current = surveyId ? await api(`/api/surveys/${surveyId}`) : { questions: [] };
    const previousIds = new Set(current.questions.map((question) => question.id));
    const retainedIds = new Set(questions.filter((question) => question.id).map((question) => question.id));
    for (const id of previousIds) {
      if (!retainedIds.has(id)) await api(`/api/questions/${id}`, { method: "DELETE" });
    }
    const orderedIds = [];
    for (const question of questions) {
      const payload = {
        questionText: question.questionText,
        questionType: question.questionType,
        required: question.required,
        options: question.options,
      };
      const result = question.id
        ? await api(`/api/questions/${question.id}`, { method: "PUT", body: JSON.stringify(payload) })
        : await api(`/api/surveys/${saved.id}/questions`, { method: "POST", body: JSON.stringify(payload) });
      orderedIds.push(result.id);
    }
    if (orderedIds.length) {
      await api(`/api/surveys/${saved.id}/questions/order`, {
        method: "PUT", body: JSON.stringify({ questionIds: orderedIds }),
      });
    }
    if (publishAfter) {
      const result = await api(`/api/surveys/${saved.id}/publish`, { method: "POST" });
      notify("Your survey is live! Copy the link from your survey list to share it.");
      navigate("/dashboard");
      if (result.publicToken) setTimeout(() => copyText(`${location.origin}/survey/${result.publicToken}`, "Survey link copied"), 150);
    } else {
      notify("Your survey has been saved.");
      navigate("/dashboard");
    }
  } catch (error) {
    notify(error.message, true);
    if (saveButton) saveButton.disabled = false;
    if (publishButton) publishButton.disabled = false;
  }
}

async function renderPublicSurvey(publicToken) {
  setLoading("Opening your survey…");
  try {
    const survey = await api(`/api/public/surveys/${encodeURIComponent(publicToken)}`);
    root.innerHTML = `
      <section class="public-wrap">
        <header class="public-header"><span class="eyebrow">A survey for you</span><h1>${escapeHtml(survey.title)}</h1><p>${escapeHtml(survey.description || "Thanks for taking a moment to share your thoughts.")}</p></header>
        <form class="public-form" id="public-form" data-token="${escapeHtml(publicToken)}">
          ${survey.questions.map(publicQuestion).join("")}
          <p id="public-error" class="form-error" role="alert"></p>
          <button class="button" type="submit">Send my response <span aria-hidden="true">→</span></button>
          <p class="small-text muted" style="margin:13px 0 0">Your answers are submitted securely and anonymously.</p>
        </form>
      </section>`;
  } catch (error) {
    root.innerHTML = `<div class="empty-state"><div class="empty-icon">✳</div><h3>This survey isn't available</h3><p>${escapeHtml(error.message === "This survey is unavailable" ? "The link may be closed or no longer available." : error.message)}</p><a class="button small" href="/">Go to Gather</a></div>`;
  }
}

function publicQuestion(question) {
  const field = `answer-${question.id}`;
  const required = question.required ? "required" : "";
  let control;
  switch (question.questionType) {
    case "SINGLE_CHOICE":
    case "YES_NO":
      control = question.options.map((option, index) => `
        <label class="answer-choice"><input type="radio" name="${field}" value="${escapeHtml(option)}" ${required && index === 0 ? "required" : ""}>${escapeHtml(option)}</label>`).join("");
      break;
    case "MULTIPLE_CHOICE":
      control = question.options.map((option) => `
        <label class="answer-choice"><input type="checkbox" name="${field}" value="${escapeHtml(option)}">${escapeHtml(option)}</label>`).join("");
      break;
    case "RATING":
      control = `<div class="rating-options">${[1, 2, 3, 4, 5].map((rating) => `<label><input type="radio" name="${field}" value="${rating}" ${required && rating === 1 ? "required" : ""}>${rating}</label>`).join("")}</div>`;
      break;
    case "LONG_TEXT":
      control = `<textarea name="${field}" maxlength="4000" ${required} placeholder="Type your answer…"></textarea>`;
      break;
    default:
      control = `<input name="${field}" type="text" maxlength="500" ${required} placeholder="Type your answer…">`;
  }
  return `<div class="public-question" data-public-question="${question.id}" data-type="${question.questionType}" data-required="${question.required}">
    <label class="question-label">${escapeHtml(question.questionText)}${question.required ? '<span class="required-star">*</span>' : ""}</label>${control}
  </div>`;
}

async function renderAnalytics(surveyId) {
  setLoading("Gathering your insights…");
  try {
    const [survey, analytics, responses] = await Promise.all([
      api(`/api/surveys/${surveyId}`),
      api(`/api/surveys/${surveyId}/analytics`),
      api(`/api/surveys/${surveyId}/responses`),
    ]);
    root.innerHTML = `
      <section class="page-heading">
        <div><button class="text-button" data-action="dashboard">← All surveys</button><h1 style="margin-top:8px">${escapeHtml(survey.title)}</h1><p>Here’s what people are saying.</p></div>
        <button class="button secondary" data-action="edit-survey" data-id="${surveyId}">Edit survey</button>
      </section>
      <section class="analytics-summary">
        <div class="stat-card"><div class="stat-label">Responses</div><div class="stat-value">${analytics.totalResponses}</div></div>
        <div class="stat-card"><div class="stat-label">Questions</div><div class="stat-value">${analytics.totalQuestions}</div></div>
        <div class="stat-card"><div class="stat-label">Average rating</div><div class="stat-value">${analytics.averageRating == null ? "—" : `${Number(analytics.averageRating).toFixed(1)} / 5`}</div></div>
      </section>
      <section><div class="section-head"><h2>Question insights</h2></div>
        ${analytics.questions.length ? analytics.questions.map(questionAnalytics).join("") : '<div class="empty-state"><h3>No questions yet</h3><p>Add a few questions to this survey to start collecting insights.</p></div>'}
      </section>
      <section style="margin-top:28px"><div class="section-head"><h2>Individual responses</h2><span class="small-text muted">${responses.length} total</span></div>
        <div class="response-list">${responses.length ? responses.map((response, index) => `
          <details class="response-card"><summary>Response ${responses.length - index} · ${new Date(response.submittedAt).toLocaleString()}</summary>
            <div class="response-answer">${response.answers.map((answer) => `<div><strong>${escapeHtml(answer.questionText)}</strong><br>${escapeHtml(Array.isArray(answer.answer) ? answer.answer.join(", ") : answer.answer)}</div>`).join("")}</div>
          </details>`).join("") : '<div class="empty-state"><h3>Your responses will show up here</h3><p>Share your survey link to start hearing from people.</p></div>'}</div>
      </section>`;
  } catch (error) {
    notify(error.message, true);
    navigate("/dashboard");
  }
}

function questionAnalytics(question) {
  const entries = Object.entries(question.distribution || {});
  const total = entries.reduce((sum, [, count]) => sum + count, 0);
  let result = "";
  if (question.questionType === "RATING") {
    result = question.averageRating == null
      ? '<p class="muted">No ratings yet.</p>'
      : `<p class="muted">Average rating</p><h2 style="color:var(--green)">${Number(question.averageRating).toFixed(1)} <span class="small-text muted">/ 5</span></h2>`;
  } else if (entries.length) {
    result = entries.sort((a, b) => b[1] - a[1]).map(([label, count]) => `
      <div class="bar-row"><span title="${escapeHtml(label)}">${escapeHtml(label)}</span><div class="bar-track"><div class="bar-fill" style="width:${Math.round(count / total * 100)}%"></div></div><strong>${count}</strong></div>`).join("");
  } else {
    result = '<p class="muted">No answers yet.</p>';
  }
  return `<article class="analytics-question"><h3>${escapeHtml(question.questionText)}</h3>${result}<p class="muted" style="margin:12px 0 0">${question.answerCount} ${question.answerCount === 1 ? "answer" : "answers"}</p></article>`;
}

async function renderAdmin() {
  setLoading("Loading admin workspace…");
  try {
    const [users, surveys] = await Promise.all([api("/api/admin/users"), api("/api/admin/surveys")]);
    root.innerHTML = `
      <section class="page-heading"><div><span class="eyebrow">Administration</span><h1>Platform overview</h1><p>Review accounts and published surveys.</p></div></section>
      <section class="section-head"><h2>Users</h2><span class="muted small-text">${users.length} accounts</span></section>
      <div class="survey-grid">${users.map((account) => `<article class="survey-card"><div class="card-top"><h3>${escapeHtml(account.name)}</h3><span class="status">${escapeHtml(account.role)}</span></div><p>${escapeHtml(account.email)}</p><div class="card-meta"><span>${new Date(account.createdAt).toLocaleDateString()}</span>${account.id !== user().id ? `<button class="icon-button" data-action="delete-user" data-id="${account.id}">Remove user ×</button>` : "<span>Your account</span>"}</div></article>`).join("") || '<div class="empty-state">No users yet.</div>'}</div>
      <section class="section-head" style="margin-top:30px"><h2>Surveys</h2><span class="muted small-text">${surveys.length} total</span></section>
      <div class="survey-grid">${surveys.map((survey) => `<article class="survey-card"><div class="card-top"><h3>${escapeHtml(survey.title)}</h3><span class="status ${escapeHtml(survey.status)}">${escapeHtml(survey.status)}</span></div><p>${escapeHtml(survey.ownerEmail)}</p><div class="card-meta"><span>${survey.responseCount} responses</span>${survey.status === "PUBLISHED" ? `<button class="icon-button" data-action="disable-survey" data-id="${survey.id}">Close survey</button>` : ""}</div></article>`).join("") || '<div class="empty-state">No surveys yet.</div>'}</div>`;
  } catch (error) {
    notify(error.message, true);
    navigate("/dashboard");
  }
}

async function copyText(text, successMessage = "Copied to clipboard") {
  try {
    await navigator.clipboard.writeText(text);
    notify(successMessage);
  } catch {
    window.prompt("Copy this link:", text);
  }
}

document.addEventListener("click", async (event) => {
  const button = event.target.closest("[data-action]");
  if (!button) return;
  const action = button.dataset.action;
  const id = Number(button.dataset.id);
  try {
    switch (action) {
      case "login": renderAuth("login"); break;
      case "register": renderAuth("register"); break;
      case "forgot-password": renderForgotPassword(); break;
      case "dashboard": navigate("/dashboard"); break;
      case "new-survey": navigate("/builder"); break;
      case "edit-survey": navigate(`/builder/${id}`); break;
      case "analytics": navigate(`/analytics/${id}`); break;
      case "logout":
        sessionStorage.removeItem(TOKEN_KEY);
        sessionStorage.removeItem(USER_KEY);
        history.replaceState({}, "", "/");
        renderLanding();
        updateNavigation();
        notify("You’re signed out. See you next time.");
        break;
      case "add-question": {
        const form = document.getElementById("builder-form");
        const data = readQuestionCards(form);
        data.push({ id: null, questionText: "", questionType: "TEXT", required: false, options: [] });
        form.querySelector("#question-list").innerHTML = data.map(questionEditor).join("");
        form.querySelectorAll("[data-question-text]")[data.length - 1]?.focus();
        break;
      }
      case "remove-question": {
        const form = document.getElementById("builder-form");
        const cardIndex = [...form.querySelectorAll("[data-question]")].indexOf(button.closest("[data-question]"));
        const fixedData = readQuestionCards(form);
        fixedData.splice(cardIndex, 1);
        form.querySelector("#question-list").innerHTML = fixedData.map(questionEditor).join("");
        break;
      }
      case "save-survey": await saveBuilder(false); break;
      case "save-publish": await saveBuilder(true); break;
      case "copy-link": await copyText(`${location.origin}/survey/${button.dataset.token}`); break;
      case "publish": {
        button.disabled = true;
        const result = await api(`/api/surveys/${id}/publish`, { method: "POST" });
        await renderDashboard();
        notify("Your survey is live. Copy its link to share it.");
        if (result.publicToken) setTimeout(() => copyText(`${location.origin}/survey/${result.publicToken}`, "Survey link copied"), 150);
        break;
      }
      case "close-survey":
        if (window.confirm("Close this survey? Respondents will no longer be able to submit answers.")) {
          await api(`/api/surveys/${id}/close`, { method: "POST" });
          notify("Survey closed.");
          await renderDashboard();
        }
        break;
      case "delete-survey":
        if (window.confirm("Delete this survey and all of its responses? This can't be undone.")) {
          await api(`/api/surveys/${id}`, { method: "DELETE" });
          notify("Survey deleted.");
          await renderDashboard();
        }
        break;
      case "delete-user":
        if (window.confirm("Delete this account and all surveys owned by this user? This can't be undone.")) {
          await api(`/api/admin/users/${id}`, { method: "DELETE" });
          notify("User deleted.");
          await renderAdmin();
        }
        break;
      case "disable-survey":
        if (window.confirm("Close this survey? Respondents will no longer be able to submit answers.")) {
          await api(`/api/admin/surveys/${id}/disable`, { method: "POST" });
          notify("Survey closed.");
          await renderAdmin();
        }
        break;
      default: break;
    }
  } catch (error) {
    notify(error.message, true);
  }
});

function readQuestionCards(form) {
  return [...form.querySelectorAll("[data-question]")].map((card) => ({
    id: card.dataset.questionId ? Number(card.dataset.questionId) : null,
    questionText: card.querySelector("[data-question-text]").value.trim(),
    questionType: card.querySelector("[data-question-type]").value,
    required: card.querySelector("[data-question-required]").checked,
    options: card.querySelector("[data-question-options]").value.split("\n").map((option) => option.trim()).filter(Boolean),
  }));
}

document.addEventListener("change", (event) => {
  if (!event.target.matches("[data-question-type]")) return;
  const card = event.target.closest("[data-question]");
  const current = readQuestionCards(document.getElementById("builder-form"));
  const index = [...document.querySelectorAll("[data-question]")].indexOf(card);
  const question = current[index];
  const optionsField = card.querySelector(".options-field");
  const needsOptions = ["SINGLE_CHOICE", "MULTIPLE_CHOICE"].includes(question.questionType);
  optionsField.hidden = !needsOptions;
  card.querySelector(".question-footer .small-text").textContent = needsOptions
    ? "Choice question" : question.questionType === "YES_NO" ? "Two fixed answer choices" : " ";
});

document.addEventListener("submit", async (event) => {
  event.preventDefault();
  if (event.target.id === "auth-form") {
    const form = event.target;
    const registering = authMode === "register";
    const submit = form.querySelector('[type="submit"]');
    const errorBox = form.querySelector("#auth-error");
    submit.disabled = true;
    errorBox.textContent = "";
    const payload = Object.fromEntries(new FormData(form).entries());
    try {
      const result = await api(registering ? "/api/auth/register" : "/api/auth/login", {
        method: "POST", body: JSON.stringify(payload),
      });
      sessionStorage.setItem(TOKEN_KEY, result.token);
      sessionStorage.setItem(USER_KEY, JSON.stringify(result.user));
      history.replaceState({}, "", "/dashboard");
      updateNavigation();
      await renderDashboard();
      notify(registering
        ? (result.emailSent ? "Welcome to Gather. Your account is ready." : "Account created, but the welcome email couldn't be sent. Check the SMTP settings.")
        : `Welcome back, ${result.user.name.split(/\s+/)[0]}.`);
    } catch (error) {
      errorBox.textContent = error.message;
      submit.disabled = false;
    }
  }
  if (event.target.id === "forgot-form") {
    const form = event.target;
    const submit = form.querySelector('[type="submit"]');
    const errorBox = form.querySelector("#forgot-error");
    const email = new FormData(form).get("email").trim();
    submit.disabled = true;
    errorBox.textContent = "";
    try {
      const result = await api("/api/auth/forgot-password", {
        method: "POST", body: JSON.stringify({ email }),
      });
      renderResetPassword(email);
      notify(result.message);
    } catch (error) {
      errorBox.textContent = error.message;
      submit.disabled = false;
    }
  }
  if (event.target.id === "reset-form") {
    const form = event.target;
    const submit = form.querySelector('[type="submit"]');
    const errorBox = form.querySelector("#reset-error");
    submit.disabled = true;
    errorBox.textContent = "";
    const payload = Object.fromEntries(new FormData(form).entries());
    try {
      const result = await api("/api/auth/reset-password", {
        method: "POST", body: JSON.stringify(payload),
      });
      renderAuth("login");
      document.getElementById("auth-email").value = payload.email;
      notify(result.message);
    } catch (error) {
      errorBox.textContent = error.message;
      submit.disabled = false;
    }
  }
  if (event.target.id === "public-form") {
    const form = event.target;
    const submit = form.querySelector('[type="submit"]');
    const errorBox = form.querySelector("#public-error");
    submit.disabled = true;
    errorBox.textContent = "";
    const answers = [];
    for (const question of form.querySelectorAll("[data-public-question]")) {
      const id = Number(question.dataset.publicQuestion);
      const type = question.dataset.type;
      const field = question.querySelector("input, textarea");
      let answer;
      if (type === "MULTIPLE_CHOICE") {
        answer = [...question.querySelectorAll("input:checked")].map((input) => input.value);
      } else if (type === "SINGLE_CHOICE" || type === "YES_NO" || type === "RATING") {
        answer = question.querySelector("input:checked")?.value ?? "";
        if (type === "RATING" && answer !== "") answer = Number(answer);
      } else {
        answer = field.value.trim();
      }
      if (Array.isArray(answer) ? answer.length > 0 : answer !== "") {
        answers.push({ questionId: id, answer });
      } else if (question.dataset.required === "true") {
        errorBox.textContent = "Please answer all required questions before submitting.";
        question.scrollIntoView({ behavior: "smooth", block: "center" });
        submit.disabled = false;
        return;
      }
    }
    try {
      await api(`/api/public/surveys/${encodeURIComponent(form.dataset.token)}/responses`, {
        method: "POST", body: JSON.stringify({ answers }),
      });
      root.innerHTML = `<section class="public-wrap"><div class="thank-you"><div class="empty-icon">✓</div><h2>Response received</h2><p>Thanks for taking the time to share your thoughts. Your answers have been submitted.</p><a class="button small" style="margin-top:20px" href="/">Visit Gather</a></div></section>`;
    } catch (error) {
      errorBox.textContent = error.message;
      submit.disabled = false;
    }
  }
});

window.addEventListener("popstate", render);
render();
