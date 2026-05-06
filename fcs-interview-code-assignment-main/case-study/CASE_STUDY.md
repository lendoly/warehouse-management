# Case Study Scenarios to discuss

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. Think about what are important considerations for this, what are previous experiences that you have you could related to this problem and elaborate some questions and considerations

**Questions you may have and considerations:**

The core challenge in fulfillment cost tracking is that many costs are shared and indirect — a single
truck run may serve multiple stores, a warehouse handles products for multiple business units, and
overhead like facility rent or utilities is not naturally attributable to any single activity.

Key challenges I would raise before defining scope:

  Granularity and attribution:
  - At what level do we need to track costs? per Warehouse, per FulfillmentUnit (a product-warehouse-store assignment), or per individual transaction? Finer granularity gives more insight but multiplies complexity and data volume significantly.
  - How do we split shared costs (e.g., a delivery route that covers three Stores)? Fixed split by   volume, weight, distance, or negotiated allocation keys? The allocation method must be agreed with   Finance before any system is built, because changing it after the fact invalidates historical comparisons.

  Timing:
  - Are costs recognized when they are committed (purchase order), when they are incurred (goods moved), or when they are settled (invoice paid)? This distinction matters for month-end accruals and for  aligning the operational view with the financial ledger.

  Master data alignment:
  - The Business Unit Code is the key identifier linking operational activity to cost centers. If the
    same code is reused after a warehouse replacement, cost reports that do not filter by date range
    will silently mix old and new warehouse costs. This must be a first-class concern in any reporting design.

  Questions I would ask to get a better definition:
  - Who are the consumers of cost data — Finance for reporting, Operations for day-to-day
    decisions, both? Their needs are often different: Finance wants accrual-based actuals, Operations
    wants cash-flow or real-time views.
  - Is there an existing cost center hierarchy that we need to map to, or are we defining a new one?
  - What is the expected volume of cost records? A small number of high-value shipments is very different from thousands of small store replenishments per day.
  - Are there regulatory or audit requirements that dictate how long cost records must be retained and
    in what format?

  From experience in my previous works, the most common failure mode is building a technically correct cost-tracking system that Finance does not trust because the allocation logic was decided unilaterally by the engineering team. Early alignment on allocation methodology — even before the first line of code — is more valuable than any technical sophistication in the data pipeline.

----

## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes from that. How would you identify, prioritize and implement these strategies?

**Questions you may have and considerations:**

Before proposing strategies I would want to understand the current cost baseline and where the largest buckets sit. Without that, optimization efforts risk being precise but irrelevant — optimizing something that represents 3% of spend while ignoring something that represents 40%.

How I would identify opportunities:

  - Start with a cost breakdown by category (labor, transport, inventory holding, overhead) and by entity (which Warehouses and Stores have the highest unit costs). Outliers — sites that are significantly more expensive than peers — are the first investigation targets.
  - Cross-reference costs with service metrics (order lead time, fill rate, return rate). A site that is cheap but has poor service quality is not truly optimized; it is externalizing costs to the customer.
  - Review capacity utilization: underutilized warehouses carry fixed costs (rent, staff) against a small revenue base. The warehouse replace operation in this system is itself a mechanism for addressing this — consolidating business units into fewer, better-positioned locations.

Potential strategies, roughly ordered by implementation effort:

  1. Inventory right-sizing: Excess stock ties up working capital and increases holding costs. Aligning replenishment quantities to actual demand patterns is usually the highest return on investment, lowest disruption intervention.
  2. Location optimization: The Location entity in the domain carries a maxCapacity and  maxNumberOfWarehouses. These constraints exist precisely because some locations are more efficient to serve than others. Analyzing which warehouse-to-store assignments (FulfillmentUnits) generate disproportionate transport cost is a direct input to consolidation decisions.
  3. Transport consolidation: Where multiple small shipments cover the same route, batching them reduces per-unit transport cost, though it may increase lead time. The acceptable trade-off must be defined by the business.
  4. Labor scheduling: Aligning staffing levels to inbound/outbound volume patterns avoids paying for idle capacity during low-demand periods.

How to prioritize:

  I would probably suggest to use a simple impact-vs-effort matrix: strategies with large cost impact and low implementation risk go first. I would avoid strategies that require significant process change before there is organizational buy-in, as they rarely deliver their projected savings.

  Questions I would ask:
  - Are there contractual constraints (e.g., long-term warehouse leases) that limit which strategies are actually available?
  - What is the acceptable impact on service levels? is there a minimum fill rate or maximum lead time that cannot be breached?
  - Is there a target cost reduction percentage, or is this an open-ended improvement program? The answer shapes whether we need quick wins or a longer-term transformation.

----

## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data. The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. What benefits the company would have from that and how would you ensure seamless integration and data synchronization?

**Questions you may have and considerations:**

The value of integration is eliminating reconciliation work and the human errors that come with it. Without integration, Finance re-enters or manually reconciles data from operational systems into the system, this is a process that is slow, error-prone, and gives decision-makers stale information.

Benefits of tight integration:
  - A single version of cost truth: operational and financial views agree, so there are no end-of-month surprises when the ledger does not match the operational dashboard.
  - Faster close: automated posting of cost events into the financial system reduces the time Finance spends on period-end reconciliation.
  - Real-time alerting: if costs for a warehouse exceed budget mid-period, the system can flag it  while there is still time to act, rather than discovering the overrun after month-end.

Integration design considerations:

  I notice the codebase already uses an event-driven pattern for the legacy Store Manager integration (StoreEvent fired after transaction commit, observed by LegacyStoreManagerGateway). The same pattern is appropriate for financial system integration: emit cost events when they are created or updated, and let the financial system consume them asynchronously. This decouples the operational system from financial system availability, if the financial system is down for maintenance, events queue up and replay when it recovers.

  Key questions before designing the integration:
  - What financial system is in use (SAP, Oracle, Workday, etc.)? Each has different integration  capabilities. Some support real-time APIs, others expect nightly batch files. The integration pattern must match what the target system can consume.
  - Who owns the data. Does the Cost Control Tool push to the financial system, or does it pull from the Cost Control Tool?   This determines where the source of truth sits and how corrections are handled.
  - How are corrections and reversals managed? Costs are sometimes posted incorrectly and need to be  reversed. The integration must support this without creating phantom entries in either system.
  - What is the acceptable data latency for reporting? "Real-time" is often a business aspiration  that hides a more nuanced requirement — Finance may be fine with 15-minute lag for dashboards but needs next-day accuracy for statutory reporting.
  - Are there data residency or compliance requirements (e.g., General Data Protection Regulation, local accounting laws) that restrict where cost data can be stored or transmitted?

----

## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what would you take into account designing a system to support accurate budgeting and forecasting?

**Questions you may have and considerations:**

Budgeting and forecasting matter because they convert operational plans into financial expectations, enabling the business to allocate capital, negotiate contracts, and set performance targets in advance rather than reacting to costs after they are incurred.

What I would take into account when designing the system:

  Historical data as the foundation:
  - Accurate forecasting requires clean historical actuals. This is why the decisions made in Scenarios 1 and 3 (cost attribution, integration quality) are prerequisites. The system should maintain cost history at whatever granularity is needed for forecasting (per warehouse, per category, per period).
  - The warehouse replace operation creates a natural data boundary: the archived warehouse's cost history belongs to the old entity and should not be used as a baseline for the new warehouse without explicit adjustment.

  Inputs beyond historical data:
  - Planned operational changes: new warehouse openings, replacements, or closures change the cost structure significantly and must be reflected in the forecast before they happen.
  - Volume forecasts: fulfillment costs are largely volume-driven (more products moved = more labor and transport). If the business has a demand forecast, it should feed directly into the cost model.
  - Known price changes: if a transport contract is renewing at a different rate, or minimum wage is changing, those need to be incorporated rather than extrapolating from current rates.

  Forecast model approach:
  - I would start simple — a driver-based model where cost = unit cost * volume driver for each major cost category. This is transparent, auditable, and Finance can adjust assumptions directly. Complex machine learning models can improve accuracy but reduce interpretability and require significantly more data to train reliably.
  - Rolling forecasts (updated monthly) are generally more useful than a single annual budget, because they remain relevant as conditions change rather than becoming obsolete by Q3.

  Questions I would ask:
  - What planning horizon is needed? one month, one quarter, one year, three years? The horizon determines how much uncertainty must be modeled and what techniques are appropriate.
  - Should the system generate alerts when actuals deviate significantly from forecast? If so, what deviation threshold triggers an alert, and who receives it?
  - Is scenario planning required? the ability to model "what if we add two more warehouses" or  "what if volume drops 20%"? This significantly increases design complexity.

----

## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. Why is it important to preserve cost history and how this relates to keeping the new Warehouse operation within budget?

**Questions you may have and considerations:**

This scenario is where the data model design has direct financial consequences. The Business Unit Code
is reused for operational continuity — routes, contracts, and store assignments can remain stable —
but from a cost accounting perspective, the old and new warehouse are distinct entities.

Why preserving cost history is critical:

  - Audit and compliance: financial records must be traceable. If costs from the old warehouse are
    mixed with costs from the new one (because they share a Business Unit Code), auditors cannot verify
    that the figures in the accounts represent what actually happened at each physical location.
  - Performance baseline: to evaluate whether the new warehouse is actually more cost-effective than
    the one it replaced, you need the old warehouse's cost profile as a benchmark. Without a clean
    separation, the comparison is meaningless.
  - Budget attribution: the replacement itself has costs (decommissioning, fit-out, transition labor).
    These should be tracked separately — ideally against a project budget — rather than absorbed into
    either the old or new warehouse's operational cost line.
  - Tax and depreciation: the old warehouse may have assets that are still being depreciated. Archiving
    the warehouse operationally does not change the financial treatment of those assets; the historical
    cost records are needed to support ongoing tax filings.

How this relates to the technical implementation:

  The archivedAt timestamp on the archived DbWarehouse entity is the critical boundary. Any cost
  record attributed to a Business Unit Code must also carry an effective date, so reports can
  unambiguously assign it to either the old (archivedAt is not null, event date <= archivedAt) or
  the new (event date > archivedAt) warehouse. A cost system that joins only on Business Unit Code
  without a date filter will silently aggregate both, producing incorrect totals.

Questions I would ask before implementing:

  - What is the planned transition period? If both the old and new warehouse operate concurrently
    during handover, there may be a window where costs need to be split between them — this is more
    complex than a clean cutover.
  - Does the new warehouse inherit any existing contracts (e.g., a transport agreement tied to the
    Business Unit Code)? If so, how are pre-existing contractual costs attributed — to the old
    entity that signed the contract, or the new one that benefits from it?
  - What is the budget for the new warehouse at launch, and how was it derived? If it was based on
    the old warehouse's cost profile without adjusting for operational differences (different size,
    location, automation level), the budget baseline will be wrong from day one.
  - Who is responsible for the transition costs — a central project budget, the old warehouse's
    cost center, or the new one? This decision should be made before the replacement begins, not
    after, to avoid disputes during month-end reporting.

----

## Instructions for Candidates
Before starting the case study, read the [BRIEFING.md](BRIEFING.md) to quickly understand the domain, entities, business rules, and other relevant details.

**Analyze the Scenarios**: Carefully analyze each scenario and consider the tasks provided. To make informed decisions about the project's scope and ensure valuable outcomes, what key information would you seek to gather before defining the boundaries of the work? Your goal is to bridge technical aspects with business value, bringing a high level discussion; no need to deep dive.
