# Air automation

1. Create a JetBrains Air Automation.
2. Select Codex Terra (recommended).
3. Copy the automation prompt from `AIR_AUTOMATIONS_INSTRUCTIONS.txt`.
4. Add a webhook trigger for branch `myName/integration`.
5. Create `~/androidxMergerAirWebhook.txt`: URL on line 1, token on line 2.
6. Run the merge script. It pushes, calls the webhook, waits for it, then pulls the branch.
