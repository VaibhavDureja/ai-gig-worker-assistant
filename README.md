# AI Assistant for Gig Workers

**Created by Vaibhav Dureja**

An early-stage Python project exploring how independent workers and small businesses can simplify routine customer-booking communication.

## What works now

The `auto_reply(name, service, date)` function returns an acknowledgement message based on information entered by the user.

This is a **rule-based prototype**. It does not currently use a generative AI model, send messages, check availability, schedule appointments, or connect to a calendar.

## Try it

Requires Python 3. No additional packages are needed.

```bash
python3 booking_reply.py
```

Example output:

```text
Hi Example Customer, thanks for your request for Plumbing on 25 January. I'll confirm shortly.
```

**Example Customer is a fictional placeholder**, not the project creator.

## Tests and quality checks

This project has three small automated tests for the reply generator. I ran them successfully with Python 3 before adding them to the repository.

```bash
python3 -m unittest discover -s tests -v
```

The GitHub Actions workflow in `.github/workflows/python-tests.yml` is configured to run these tests on pushes and pull requests. Its first GitHub-hosted execution should be checked on the repository's **Actions** tab; local test success does not guarantee the hosted workflow has passed.

## Project roadmap

- [x] Basic booking acknowledgement generator
- [ ] Validate booking inputs and handle missing details
- [x] Add automated tests for basic message formatting
- [ ] Create a simple interface for entering booking requests
- [ ] Add calendar availability checks
- [ ] Explore AI-assisted customer-request categorisation with appropriate human review

These are proposed improvements, not completed features.

## About the creator

I'm **Vaibhav Dureja**, based in London, UK. My background combines international business, operations, digital ventures, and research into emerging technologies.

- **MSc International Business (Merit)** — Birkbeck, University of London
- **BBA** — Amity University Haryana
- Former **Chair of Student Parliament** and **Chair of Scrutiny Panel** at Birkbeck
- Professional experience supporting operations across multiple London accommodation properties
- Founder of **Mooncommerce Limited**
- Exploring **SupplyCorridor**, an early-stage B2B international trade platform concept
- Research interests: responsible AI, digital transformation, blockchain, IoT, and business processes

My MSc research examined **emerging technologies and their implications for international business**, focusing on AI, blockchain, and IoT.

**Research working paper:** [SSRN](https://ssrn.com/abstract=6111026)  
**GitHub:** [VaibhavDureja](https://github.com/VaibhavDureja)

## Project note

This repository demonstrates a small automation concept. It should not be described as a deployed AI product or a working booking platform.
