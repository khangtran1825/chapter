window.addEventListener("pageshow", (event) => {
  if (
    event.persisted &&
    /^\/(account|logout|cart|checkout|orders|admin)(\/|$)/.test(
      location.pathname,
    )
  ) {
    location.reload();
  }
});

const errorSummary = document.querySelector("[role=alert]");
if (errorSummary) {
  errorSummary.setAttribute("tabindex", "-1");
  errorSummary.focus();
}
