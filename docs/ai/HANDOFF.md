# Hand Off

No pending tasks. Last cleared: 2026-04-01.

## Recently Completed
- **Issue #61 finished** — Fixed intermittent summary language bug:
  - Whisper API language detection now captured and stored
  - Summary generation uses explicit language instruction in prompt
  - Eliminates LLM guessing language from text content
- **Issue #63 finished** — Main screen redesign
- **Issue #60 finished** — Settings improvements
- **Issue #59 finished** — minSdk bump to 30
- **Issue #62 cleaned up** — Removed broken OpenRouter STT code

## Architecture Insights from Recent Work
- OpenRouter does NOT have a dedicated STT endpoint - requires base64 chat API approach
- Whisper language detection is reliable and should always be captured
- Language-aware prompts prevent misclassification (German→English)
- Navigation-aware refresh pattern using currentBackStackEntryAsState()
- Prompt logging should use {{TEXT}} placeholder for privacy
