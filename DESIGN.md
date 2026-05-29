# Design

## Source of truth
- Status: Draft
- Last refreshed: 2026-05-29
- Primary product surfaces: dashboard, trip planner, settlement, community, post detail, profile, auth/invite callbacks, modal workflows.
- Evidence reviewed:
  - `README.md`
  - `frontend/README.md`
  - `frontend/AGENTS.md`
  - `frontend/src/index.css`
  - `frontend/tailwind.config.ts`
  - `frontend/public/tribe-logo.png`
  - `frontend/public/tribe-textlogo.png`
  - `frontend/src/App.tsx`
  - `frontend/src/components/Header.tsx`
  - `frontend/src/components/ui/button.tsx`
  - `frontend/src/pages/Dashboard.tsx`
  - `docs/contracts/api.md`
  - `docs/runbooks/local-harness.md`
  - `/Users/wingwogus/Downloads/DESIGN.md`

This document adapts the downloaded Apple design analysis into a Tribe-specific design contract. Keep the Apple-derived discipline of quiet chrome, strong imagery, clear section rhythm, and one primary action color, but do not copy Apple branding, product-category assumptions, or proprietary visual details.

## Token snapshot

```yaml
version: alpha
name: Tribe-design-system
description: >
  A collaborative travel planning interface with quiet utility chrome, map and
  itinerary content as the hero, and a single icon-derived primary blue for
  navigation, calls to action, focus, selection, and realtime state.

colors:
  primary: "hsl(225 75% 62%)" # #557AE7, app icon blue
  primary-hex: "#557AE7"
  primary-glow: "hsl(218 85% 72%)" # #7BA7F4, app icon highlight
  primary-on-dark: "#9DB9FF"
  primary-soft: "hsl(225 75% 62% / 0.10)"
  on-primary: "#FFFFFF"

  ink: "#1F2937"
  body: "#1F2937"
  body-muted: "#64748B"
  body-subtle: "#94A3B8"
  body-on-dark: "#FFFFFF"

  canvas: "#FFFFFF"
  canvas-soft: "#F8FAFC"
  canvas-parchment: "#F5F7FA"
  surface-card: "#FFFFFF"
  surface-dark: "#111827"
  surface-dark-soft: "#1F2937"

  border: "#E5E7EB"
  divider-soft: "#F1F5F9"
  input: "#E5E7EB"

  success: "hsl(142 76% 48%)"
  destructive: "hsl(0 84% 60%)"

typography:
  display:
    fontFamily: "system-ui, -apple-system, BlinkMacSystemFont, Inter, sans-serif"
    fontSize: "40px"
    fontWeight: 700
    lineHeight: 1.1
    letterSpacing: "0"
  page-title:
    fontFamily: "system-ui, -apple-system, BlinkMacSystemFont, Inter, sans-serif"
    fontSize: "32px"
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: "0"
  section-title:
    fontFamily: "system-ui, -apple-system, BlinkMacSystemFont, Inter, sans-serif"
    fontSize: "24px"
    fontWeight: 600
    lineHeight: 1.25
    letterSpacing: "0"
  body:
    fontFamily: "system-ui, -apple-system, BlinkMacSystemFont, Inter, sans-serif"
    fontSize: "16px"
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: "0"
  body-strong:
    fontFamily: "system-ui, -apple-system, BlinkMacSystemFont, Inter, sans-serif"
    fontSize: "16px"
    fontWeight: 600
    lineHeight: 1.45
    letterSpacing: "0"
  caption:
    fontFamily: "system-ui, -apple-system, BlinkMacSystemFont, Inter, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: 1.43
    letterSpacing: "0"
  micro:
    fontFamily: "system-ui, -apple-system, BlinkMacSystemFont, Inter, sans-serif"
    fontSize: "12px"
    fontWeight: 400
    lineHeight: 1.33
    letterSpacing: "0"

rounded:
  none: "0px"
  sm: "4px"
  md: "6px"
  lg: "8px"
  pill: "9999px"
  full: "9999px"

spacing:
  xxs: "4px"
  xs: "8px"
  sm: "12px"
  md: "16px"
  lg: "24px"
  xl: "32px"
  xxl: "48px"
  section: "80px"
```

## Brand
- Personality: calm, collaborative, travel-aware, practical, and optimistic.
- Trust signals: realtime updates, visible member state, transparent settlement math, route-aware planning, clear API-backed data, and recoverable empty/error states.
- Avoid: generic travel-agency gloss, decorative gradients as the main visual idea, heavy card shadows, nested cards, fake fixture-backed product data, playful clutter inside planning tools, and any second brand-level action color.

## Product goals
- Goals:
  - Help groups create, edit, discuss, settle, and reuse trips in one continuous flow.
  - Make trip state legible at a glance: dates, region, members, itinerary, route, expenses, chat, and review status.
  - Keep high-frequency planning surfaces efficient, dense, and predictable.
  - Make community reuse feel connected to real trip planning rather than a separate content island.
- Non-goals:
  - Do not turn core app screens into marketing landing pages.
  - Do not prioritize decorative presentation over itinerary, map, chat, and settlement clarity.
  - Do not introduce a second design system or UI dependency when shadcn/ui, Tailwind tokens, and existing components can be extended.
- Success signals:
  - Users can identify the current trip, next action, collaborators, route context, and outstanding settlement state without reading help text.
  - Primary action usage is visually consistent across dashboard, planner, settlement, community, and modal flows.
  - New frontend work can cite this file for color, spacing, component, state, and verification decisions.

## Personas and jobs
- Primary personas:
  - Trip organizer coordinating destination, dates, members, and itinerary sequence.
  - Trip participant checking plans, chatting, adding places, and joining expenses.
  - Post-trip sharer converting the finished trip into a review or reusable community post.
- User jobs:
  - Create or join a trip.
  - Search places, add itinerary items, reorder plans, and understand travel time.
  - Discuss decisions in trip chat without losing planning context.
  - Enter expenses and understand who owes whom.
  - Generate and share a useful trip review.
  - Import a public trip flow into a private trip.
- Key contexts of use:
  - Desktop planning before the trip.
  - Mobile checks during travel.
  - Post-trip review, settlement, and sharing.

## Information architecture
- Primary navigation:
  - Global header with Tribe logo, dashboard/community switch, join trip action, and account menu.
  - Trip-level navigation should keep itinerary, map, chat, members, expense, and review actions close to the active trip context.
- Core routes/screens:
  - `/`: dashboard and trip list.
  - `/signup`: onboarding.
  - `/oauth/callback`: OAuth completion.
  - `/invite`: invite token handling.
  - `/trip/:tripId`: planner, place search, itinerary, realtime sync, chat, members.
  - `/settlement/:tripId`: daily and total settlement.
  - `/community`: public trip posts.
  - `/post/:postId`: community post detail and import.
  - `/profile/:memberId`: member profile.
- Content hierarchy:
  - App identity and route context first.
  - Current trip or current post title second.
  - Primary action third.
  - Supporting metadata, filters, and secondary actions after the main task surface.

## Design principles
- Principle 1: The trip is the artifact. UI chrome should frame the itinerary, map, settlement, and community content rather than compete with it.
- Principle 2: One blue means action. The app icon blue is the primary interactive signal across buttons, links, selected states, focus, loading, and realtime highlights.
- Principle 3: Dense where work happens, airy where orientation happens. Dashboard and community can breathe; planner and settlement should favor scanning, comparison, and repeated action.
- Principle 4: State must be explicit. Loading, empty, error, success, disabled, offline, and slow-network states are part of the design, not afterthoughts.
- Tradeoffs:
  - Follow the Apple template's restraint and single-primary discipline, but keep Tribe's app surfaces more operational than showroom-like.
  - Gradients may remain as subdued transitional treatments in existing screens, but new work should prefer flat surfaces, primary tints, and content imagery unless a gradient directly supports the travel context.
  - Use cards for repeated items and modals; do not wrap entire page sections in decorative cards.

## Visual language
- Color:
  - Primary is the app icon blue: `hsl(225 75% 62%)` / `#557AE7`.
  - `primary-glow` is the app icon highlight: `hsl(218 85% 72%)` / `#7BA7F4`.
  - Primary owns CTAs, active nav, selected states, focus rings, progress, spinners, and important inline links.
  - Do not introduce a second brand color. If emphasis is needed, use primary tint, neutral contrast, or a semantic token.
  - Success, destructive, and category colors are semantic. They must not become decorative brand colors.
  - Use white, soft slate, and parchment-like surfaces to create section rhythm. Use dark surfaces sparingly for maps, media, or strong contrast moments.
- Typography:
  - Use the existing system font stack.
  - Keep letter spacing at `0`.
  - Use hero/display sizes only on orientation surfaces, not inside dense planner panels or compact cards.
  - Body copy should be readable at 16px with 1.45-1.5 line height.
- Spacing/layout rhythm:
  - Structural spacing follows an 8px base: 8, 16, 24, 32, 48, 80.
  - Dashboard body gutters must align with the global header container: `container mx-auto px-4 md:px-6`.
  - Dashboard/community layouts can use 24px gutters and 32px section rhythm.
  - Dashboard trip-list sections use a compact 48px vertical rhythm between groups, not hero-scale spacing.
  - Planner and settlement tools can tighten to 8-16px internal spacing where scanning matters.
  - The Tailwind container max width remains `1400px`.
- Shape/radius/elevation:
  - Standard component radius is 8px or less, matching the current `--radius: 0.5rem`.
  - Dashboard trip cards are the current visual exception: use a 24px radius to match the photo-led travel card reference.
  - Pills are reserved for primary CTAs, compact status chips, search/filter chips, and avatar/action clusters.
  - Shadows should be soft and functional. Prefer border, tint, and position over heavy elevation.
- Motion:
  - Motion should confirm state changes, not decorate.
  - Use short transitions for hover, selected state, dialog entry, drag/reorder, and realtime updates.
  - Respect reduced-motion preferences for non-essential animation.
- Imagery/iconography:
  - Use the Tribe logo and text logo from `frontend/public`.
  - Use real trip/place imagery when available; do not use generic decorative travel art as a substitute for user data.
  - Use lucide icons for actions and affordances.
  - The airplane-heart icon color is the primary color source.

## Components
- Existing components to reuse:
  - shadcn/ui primitives under `frontend/src/components/ui`.
  - `Button`, `Card`, `Dialog`, `DropdownMenu`, `Avatar`, `Toast`, `Tabs`, `Select`, `Input`, `Textarea`, `Calendar`, `Badge`, `Tooltip`.
  - App-specific components including `Header`, trip modals, settlement modals, chat modal, itinerary map, place search/detail surfaces, post cards, and review modal.
- New/changed components:
  - Prefer extracting repeated planner/settlement patterns only when duplication is meaningful and behavior is shared.
  - New components should consume Tailwind semantic tokens (`primary`, `muted`, `border`, `background`, `card`) rather than inline hex.
  - Header primary actions such as `로그인` use a flat primary fill with pill radius; do not use gradients in the header CTA area.
  - Header route-switch ghost actions use rounded neutral pills with subtle hover tint, not bordered or gradient buttons.
  - Dashboard trip cards should stay compact and rounded: about 320px per card on desktop, 144px image height, 24px card radius, 16px card title, 14px metadata, and restrained elevation.
  - Dashboard action buttons sit in the body header, not the global header: `여행 참여하기` is a compact outline pill with a users icon, and `새 여행 만들기` is a compact flat primary pill with a plus icon.
  - Dashboard trip-list section labels are `다가오는 여행` for active/future trips and `완료된 여행` for trips whose end date has passed.
  - Dashboard trip-list sections with no cards are hidden. When there are no trips at all, show one full-width rounded empty state with a circular muted inline SVG takeoff-airplane silhouette using smoothed paths, concise title/subtitle, and a compact primary `새 여행 추가` action.
- Variants and states:
  - Primary button: `bg-primary text-primary-foreground`; use for one dominant action in a local region.
  - Secondary/outline button: border or muted surface; use for reversible or alternate actions.
  - Ghost/icon button: quiet toolbar and row actions; icon-only buttons need accessible labels/tooltips where appropriate.
  - Selected state: primary border plus primary tint, not a new color.
  - Destructive state: destructive token only for irreversible actions.
- Token/component ownership:
  - Global tokens live in `frontend/src/index.css`.
  - Tailwind token mapping lives in `frontend/tailwind.config.ts`.
  - Component variants live in the component file that owns the abstraction, usually under `frontend/src/components/ui` or an app-specific component.

## Accessibility
- Target standard: WCAG 2.1 AA for contrast, keyboard access, focus visibility, and semantic structure.
- Keyboard/focus behavior:
  - Every interactive element must be reachable by keyboard.
  - Focus rings use the primary/ring token and must not be removed.
  - Dialogs must trap focus and restore focus on close.
- Contrast/readability:
  - Primary blue on white requires adequate text size/weight or `primary-foreground`.
  - Muted text must remain readable on `canvas-soft` and `card`.
  - Do not place text over busy imagery without a contrast strategy.
- Screen-reader semantics:
  - Icon-only controls require labels.
  - Loading and async status should be announced where it changes task outcome.
  - Map and itinerary panels need text alternatives for route and place context.
- Reduced motion and sensory considerations:
  - Avoid persistent decorative motion.
  - Provide static, readable state for route, settlement, and sync feedback.

## Responsive behavior
- Supported breakpoints/devices:
  - Mobile: single-column, touch-first, compact header actions.
  - Tablet: two-column where content benefits from side-by-side comparison.
  - Desktop: dense planner layouts, map/detail splits, and restrained dashboard/community grids.
  - Wide desktop: content locks to existing container max width unless the map/planner surface needs a full-viewport tool layout.
- Layout adaptations:
  - Dashboard trip cards: 1 column mobile, 2 columns tablet/desktop; do not stretch the home trip list into a full-width 3-column grid without a product reason.
  - Planner: prioritize current day/itinerary and map; secondary panels collapse into sheets/drawers on narrow screens.
  - Modals: become full-width or bottom-sheet-like on mobile when forms are long.
  - Header: keep logo visible; collapse text labels before hiding core actions.
- Touch/hover differences:
  - Hover-only row actions need a mobile-visible equivalent.
  - Minimum touch target is 44px for primary actions and icon buttons.

## Interaction states
- Loading:
  - Use primary spinner/progress for blocking loads.
  - Prefer skeletons for repeated cards/lists where shape is known.
  - Keep route-level loading copy short and specific.
- Empty:
  - Empty states should show the next best action, such as create trip, join trip, add place, add expense, or share post.
  - Avoid marketing copy in operational empty states.
- Error:
  - Use the shared error envelope and localized message behavior from `docs/contracts/errors.md`.
  - Give recovery actions when possible: retry, back to dashboard, reopen modal, or contact organizer.
- Success:
  - Use toast or inline confirmation for create/update/delete/import/join actions.
  - Do not over-animate success states; keep the user in flow.
- Disabled:
  - Disabled controls need clear surrounding context for why action is unavailable.
  - Use token opacity, not a new gray palette.
- Offline/slow network:
  - Realtime and chat surfaces should communicate reconnecting/stale states.
  - Preserve typed input where possible during retry.

## Content voice
- Tone: concise, helpful, calm, and action-oriented.
- Terminology:
  - Use "trip", "itinerary", "place", "member", "expense", "settlement", "chat", "review", and "community" consistently in code and English-facing docs.
  - Korean UI copy should use short verbs and concrete nouns: create, join, add, edit, settle, import, share.
- Microcopy rules:
  - Do not explain obvious UI mechanics in visible app text.
  - Error text should state what failed and the user's next step.
  - Success text should confirm the changed object.
  - Avoid exclamation-heavy copy in core planning and settlement flows.

## Implementation constraints
- Framework/styling system:
  - React 18, TypeScript, Vite, React Router, TanStack Query, Tailwind CSS, shadcn/ui, SockJS/STOMP, Leaflet.
  - Reuse existing shadcn/ui and lucide patterns.
  - Do not add a new frontend production dependency without explicit request.
- Design-token constraints:
  - Keep global colors in `frontend/src/index.css`; all colors remain HSL variables where they are Tailwind tokens.
  - Keep Tailwind token access through `frontend/tailwind.config.ts`.
  - New UI should use semantic classes such as `bg-primary`, `text-muted-foreground`, `border-border`, and `bg-card`.
  - Do not inline primary hex in components unless a third-party API requires a raw color string.
- Data constraints:
  - Runtime screens must consume backend data through `src/api/*` and WebSocket surfaces.
  - Do not ship fixture-backed or hard-coded trip, itinerary, chat, expense, settlement, review, place, or member records in production UI paths.
- Performance constraints:
  - Planner, map, chat, and settlement screens must stay responsive under growing trip data.
  - Avoid unnecessary re-render loops in realtime handlers.
  - Lazy-load large imagery below the fold when it is not required for first meaningful paint.
- Compatibility constraints:
  - Keep SPA routes aligned with `frontend/src/App.tsx` and `frontend/vercel.json`.
  - Keep public API/realtime assumptions aligned with `docs/contracts/api.md`.
- Test/screenshot expectations:
  - Frontend changes should run `npm run lint`, `npm run typecheck`, and `npm run build` from `frontend/` when practical.
  - Map, drag/drop, route, modal, auth, or realtime behavior changes require a documented manual smoke when automation does not cover the visual behavior.

## Development workflow for future UI work
- Start by reading this file and the relevant nested guidance:
  - frontend work: `frontend/AGENTS.md`
  - API/realtime shape: `docs/contracts/api.md`
  - local checks: `docs/runbooks/local-harness.md`
- Before choosing a visual pattern, cite the relevant sections:
  - Color decisions: `Visual language > Color` and `Token snapshot`.
  - Component decisions: `Components`.
  - Layout decisions: `Responsive behavior` and `Visual language > Spacing/layout rhythm`.
  - Copy/state decisions: `Interaction states` and `Content voice`.
- During implementation:
  - Reuse existing components and tokens first.
  - Keep one dominant primary action per local region.
  - Use the app icon primary blue for action/selection/focus. Use primary tints for secondary emphasis instead of adding another hue.
  - If a needed pattern contradicts this document, update this document or add an open question before coding around it.
- During review:
  - Check that primary, radius, spacing, state handling, accessibility, and responsive behavior match this document.
  - Verify no new hard-coded product data entered runtime UI.
  - Report any skipped lint/typecheck/build/manual smoke and the residual risk.
- When updating design:
  - Keep changes small and evidence-backed.
  - Update `frontend/src/index.css` and `frontend/tailwind.config.ts` only when tokens actually change.
  - Record unresolved product/design decisions in `Open questions`.

## Open questions
- [ ] Should current gradient-heavy surfaces be gradually flattened to match this Apple-derived restraint, or kept as Tribe's current visual signature?
- [ ] What imagery standard should community posts use when a trip has no place photos?
- [ ] What exact mobile planner layout should become canonical: map-first, itinerary-first, or tabbed?
- [ ] Should dark mode be a supported production surface or remain a token-compatible fallback?
