# Publishing the personal website

This repository branch stores the website source separately from the live Cloudflare Pages deployment. The existing deployment was originally made with **Direct Upload**, not Git integration.

## Previously used infrastructure

- Domain: **vaibhavdureja.com**
- Registrar and configured DNS/forwarding: **GoDaddy**
- Previously deployed Cloudflare Pages URL: **https://vaibhadureja.pages.dev** (the missing second `v` in `vaibha` is intentional)
- Earlier GoDaddy `www` CNAME target: **vaibhadureja.pages.dev**
- Earlier root-domain forwarding: permanent redirect to **https://www.vaibhavdureja.com**
- Deployment source: ZIP with static site files

These details describe the earlier setup and are not a live DNS audit.

## Update the existing Cloudflare Pages project

1. Sign in to **https://dash.cloudflare.com/**.
2. Open **Workers & Pages** and locate the **existing** Pages project that serves `vaibhadureja.pages.dev`.
3. Choose **Create deployment** / **Upload assets** on that project.
4. Upload the Cloudflare-ready ZIP built from the files in [website/](website/). Make sure `index.html`, `_headers`, `assets/` and `supplycorridor/` are at the ZIP root.
5. Confirm the deployment. Check the production Pages URL, `/supplycorridor/`, and the custom domain.

**Do not** create a new Pages project, change Cloudflare nameservers, delete the old project, or modify the GoDaddy forwarding/CNAME when merely updating HTML/CSS.

## Relationship to GitHub

Cloudflare documents that an existing Direct Upload project cannot simply be switched to Git integration. To automate future deployments on this project, configure Wrangler with scoped Cloudflare credentials or migrate to a **new** Git-connected Pages project after separately planning the domain cutover.

GitHub profile: A public repository named `VaibhavDureja` (exact account username) must exist for GitHub to display a profile README. Its source is prepared in [github-profile/README.md](github-profile/README.md). Creating a copy on another branch of this repository does not activate the special profile display.

## Before production deployment

- Compare source against the currently live site, if it has changed since the previous ZIP.
- Check the publicly visible contact email and links.
- Keep the original ZIP as a rollback artifact.
- Check DNS and HTTPS without changing them unless actually needed.
