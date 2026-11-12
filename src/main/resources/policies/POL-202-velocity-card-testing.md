---
documentId: POL-202
title: Velocity Limits and Card Testing Controls
---

## 1. Velocity Thresholds

A card that attempts more than 6 authorisations within a rolling 60-minute
window, or more than 25 within 24 hours, exceeds the standard velocity envelope
and the transaction stream must be treated as high risk. Disputed amounts
exceeding 4x the cardholder's 30-day average ticket are anomalous; amounts
exceeding 10x are strong fraud indicators.

## 2. Card Testing Detection

A card-testing pattern is defined as 4 or more small-value declined attempts
(under $2.00) on a single card within any rolling 4-hour window, OR 2 or more
small-value declines spread across 3 or more distinct merchants. Confirmed
card-testing patterns preceding a disputed charge raise the presumption that
the card data was compromised.

## 3. Mandatory Response

When velocity limits are breached or a card-testing pattern is confirmed, the
dispute must be classified as high risk and the case escalated to a senior
investigator. The card must also be flagged for reissue review regardless of
the dispute outcome.
