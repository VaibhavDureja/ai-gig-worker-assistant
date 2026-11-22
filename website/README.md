# Vaibhav Dureja — professional portfolio website

This repository contains the source files for a **static professional portfolio** created for Vaibhav Dureja. It presents academic research, early-stage ideas, and a small public Python automation prototype without overstating their development status.

## Pages

- `/index.html` — professional homepage, research, experience and linked code project
- `/supplycorridor/index.html` — early-stage SupplyCorridor business and AI-safety research concept
- `/assets/style.css` — responsive styles
- `/assets/favicon.svg` — site icon
- `/404.html` — not-found page
- `/_headers` — Cloudflare Pages response headers

## Running the website locally

No installation, server-side application or package manager is needed. For a quick preview, open `index.html` in a browser. For a more realistic local test, from this directory run:

```bash
python3 -m http.server 8000
```

Then visit `http://localhost:8000/`.

## Publishing

**GitHub:** Create a new public repository such as `personal-portfolio` and upload the files from this directory to its root. GitHub itself does not automatically publish a website simply because files are uploaded.

**Cloudflare Pages:** Connect that repository from Cloudflare Pages (a static HTML site needs no build command and the output directory can be the repository root). Do not replace an existing production project without confirming the active deployment configuration and domain settings first.

This ZIP is a **source-code package**, not proof that the live site or DNS was updated. The original site's contact email and other existing public links have been preserved; verify them before deployment.

## Content accuracy

- The booking assistant is a **rule-based Python prototype**, not a deployed AI application.
- SupplyCorridor is an **early-stage concept**, not a live supplier verification or transaction platform.
- The referenced SSRN work is a **public working paper**, not claimed as peer-reviewed.

## External links

- [GitHub profile](https://github.com/VaibhavDureja)
- [AI Gig Worker Assistant source](https://github.com/VaibhavDureja/ai-gig-worker-assistant)
- [Working paper on SSRN](https://ssrn.com/abstract=6111026)