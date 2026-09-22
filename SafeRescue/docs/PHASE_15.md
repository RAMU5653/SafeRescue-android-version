# SafeRescue Phase 15 — Reports

## Scope
Phase 15 adds local PDF incident report generation from the existing incident timeline and risk state.

## Included
- Native Android PdfDocument report generation.
- Incident ID, generation time, risk score/severity, timeline, location availability, voice-monitoring flag.
- Report saved under app-private `filesDir/reports`.
- Raw camera/voice evidence is never embedded in the PDF.
- Reports contain a clear limitation statement and do not claim external delivery without backend confirmation.

## Security
- No public/external storage is used.
- Incident IDs are sanitized for filenames.
- The report is metadata-oriented; encrypted evidence remains in its existing lifecycle.

## Limitation
The current phase generates and stores the PDF locally. External report upload, police report submission, and delivery confirmation remain backend-dependent and are not fabricated.
