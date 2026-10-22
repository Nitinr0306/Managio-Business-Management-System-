import { z } from "zod";

/**
 * Zod 4 normally probes whether it may compile validators with `new Function`. The Content Security Policy forbids
 * that (no 'unsafe-eval' in production), and although Zod falls back safely, the probe still files a CSP violation
 * report. Validating without compilation is fast enough for these forms and keeps the policy reports clean.
 */
z.config({ jitless: true });
