# Wear & Wash App Requirements

```text
Wear & Wash
Track what you wear. Know when to wash.
```

## Overview

The app is a native Android mobile app for tracking clothing and other washable household items. Users can register items, track how often they are used, determine when they need washing, manage a laundry basket, and keep usage and washing history over time.

The app is intended for anyone who installs it and wants to manage their own clothing or washable items. There is no login requirement for the initial version.

The app should be positioned as a wardrobe care and laundry readiness companion, not as a generic wardrobe styling app. Existing apps such as Whering, Acloset, and Indyx already focus strongly on digital closets, outfit planning, styling, and AI outfit recommendations. This app should differentiate itself through laundry tracking, washing rules, event preparation, household washable items, and service partnerships.

Suggested positioning:

```text
The laundry and care companion for your wardrobe.
```

## Platform

- Native Android app
- Kotlin
- Jetpack Compose
- Local-first data storage
- No user login for now
- Google Drive backup is desired, but not required for the first version

## Implementation Strategy

The first implementation should be an Android app.

The Android version should be used to validate:

- Whether users consistently register clothing and washable items
- Whether usage tracking becomes a habit
- Whether laundry basket workflows are useful
- Whether event preparation reminders solve a real problem
- Whether users value AI-assisted item registration
- Whether laundry service partnerships can convert into revenue
- Whether users are willing to pay for premium features

If the app proves to be a business opportunity, an iPhone version should be created.

To prepare for a future iOS version:

- Keep business rules independent from Android UI code where practical.
- Keep the data model well documented.
- Avoid Android-only assumptions in the product logic.
- Use clear API boundaries if a backend is introduced.
- Keep localization keys and domain concepts consistent across platforms.
- Document washing criteria, status transitions, event preparation logic, and premium feature rules clearly.

The initial Android app does not need to be cross-platform. Native Android with Kotlin and Jetpack Compose remains the preferred MVP approach because it allows faster focus, good Android UX, and simpler first implementation.

## Languages and Internationalization

The app should support multiple languages from the beginning.

Initial supported languages:

- English
- Spanish
- Portuguese from Brazil

Internationalization requirements:

- All user-facing text should use localized string resources.
- Notifications should be localized.
- Default categories should be localized.
- Default fabrics should be localized.
- Default seasons should be localized.
- Default colors should be localized.
- Washing criteria labels should be localized.
- Status and badge labels should be localized.
- Event reminder text should be localized.
- Partner/laundry service UI text should be localized.

User-created values, such as custom categories, custom fabrics, custom colors, item names, descriptions, and comments, should remain exactly as the user typed them.

The app should allow users to follow the device language by default. A future setting may allow choosing the app language manually.

## Competitive Positioning

Several wardrobe apps already exist, including Whering, Acloset, and Indyx.

These apps are strong in areas such as:

- Digital wardrobe creation
- Outfit planning
- AI outfit suggestions
- Cost-per-wear analytics
- Wardrobe visualization
- Packing lists
- Styling services
- Social wardrobe features
- Shopping and wishlist support

This app should not try to compete directly as another generic outfit planner or AI stylist.

The main product gap is wardrobe care and laundry operations:

- Usage count since last wash
- Washing count
- Last washing date
- Washing criteria by usage, date, usage-or-date, or manual decision
- Laundry basket
- `Needs washing` badges
- Event preparation reminders
- Resetting item status and usage count after washing
- Tracking household washable items such as bedding, towels, curtains, and other textiles
- Laundry service referrals and partnerships

Competitor framing:

| App | Main Strength | Differentiation Opportunity |
| --- | --- | --- |
| Whering | Digital wardrobe, outfit planning, social styling, cost per wear | Focus on laundry readiness and care cycles instead of styling |
| Acloset | AI closet registration, AI outfit suggestions, weather/schedule-based outfits | Use AI registration as a convenience feature, but make washing/care the core |
| Indyx | Digital wardrobe, human stylist services, wardrobe analytics, reselling | Avoid stylist-service competition and focus on practical care workflows |

The clearest wedge is:

```text
Existing apps help users decide what to wear.
This app helps users know what is clean, what needs washing, what must be ready, and when to send it to laundry.
```

The MVP should prove that users value clothing care, laundry readiness, and event preparation before expanding into broader wardrobe styling or shopping recommendations.

## Supported Item Types

The app should support clothing and other washable items, including:

- Clothing
- Bedding
- Curtains
- Towels
- Other user-defined washable item categories

Categories, fabrics, colors, and seasons should support both predefined values and user-created custom values.

## Item Registration

Users should be able to register each item with:

- Name
- Category
- Color
- Brand
- Optional photo
- Fabric
- Season
- Purchase date
- Optional purchase price
- Description
- Initial usage count, including values greater than 0
- Initial washing count
- Last washing date
- Washing criteria

The app should allow editing and deleting registered items.

## Usage Tracking

Users should be able to record usage in two ways:

- Mark an item as used today
- Add usage for a custom date

Usage tracking should support:

- Usage count since last wash
- Lifetime usage count
- Optional notes per usage event
- Undoing or deleting usage history entries

The app should allow an item to be created with an existing usage count greater than 0, so users can register items they already own and have used.

## Washing Tracking

Users should be able to record washing events.

When an item is marked as washed:

- Washing count increases
- Last washing date updates
- Usage count since last wash resets
- Lifetime usage count remains unchanged
- Item status becomes clean
- Item is removed from the laundry basket, if present
- A wash event is added to the item history

The app should track both:

- Usage count since last wash
- Lifetime usage count

## Washing Criteria

The app should support these washing criteria:

- By usage count: wash after N uses
- By date: wash every N days
- By usage or date: wash when either threshold is reached
- Manual: user decides when the item should be washed

Manual or out-of-cycle washing should allow the user to add a comment explaining why the item is being washed outside its normal cycle.

Examples:

- Stained
- Sweaty
- Odor
- Travel use
- Seasonal storage
- User-defined comment

No special laundry cycle types are required for now. The app does not need to distinguish between normal wash, delicate wash, dry clean, hand wash, or similar laundry types in the MVP.

## Laundry Basket

The app should include a laundry basket/list.

Users should be able to:

- Add items to the laundry basket
- See items that automatically need washing
- See why each item needs washing
- Remove items from the laundry basket
- Mark one item as washed
- Mark multiple selected items as washed
- Mark all basket items as washed

When items are washed from the laundry basket, their wash event data should be updated and the basket entries cleared.

## Item Statuses

The app should support item statuses such as:

- Clean
- Worn
- Needs washing
- In laundry basket
- Archived

`Washed` should be treated as an event rather than a long-term status. After washing, an item becomes `Clean`.

## Badges

Items should display badges to make their state visible in lists and detail screens.

Important badges include:

- Clean
- Worn
- Needs washing
- In laundry basket
- Manual cycle
- Out of cycle
- Seasonal

The most important badge is `Needs washing`.

## Search and Filtering

The app should support searching and filtering by:

- Clothing/item name
- Category

Future filters may include:

- Status
- Color
- Brand
- Fabric
- Season
- Needs washing
- In laundry basket

## Notifications

The app should support optional notifications.

Possible notifications:

- Items need washing
- Item exceeded usage threshold
- Item exceeded date threshold
- Laundry basket has items waiting
- Reminder to log usage
- Upcoming event reminder to send planned items to laundry

## Photos

Item photos should be optional.

The app should eventually support:

- Taking a photo with the camera
- Selecting a photo from the gallery

## Backup

The desired backup approach is Google Drive backup.

For the first version, backup is not required.

Future backup support should likely include:

- Manual export to Google Drive
- Manual restore from Google Drive
- Optional automatic scheduled backup later

## Laundry Service Partnerships

The app may eventually integrate with laundry services as a business model.

The app should remain free for users. Revenue may come from partnerships with laundry services when users send laundry through the app or contact a partner service from the app.

Possible revenue models:

- Referral fee per new customer sent to a laundry service
- Commission per completed laundry order
- Featured placement for partner laundries
- Subscription or SaaS fee for laundry businesses to receive orders
- Sponsored discounts or coupons
- White-label version for laundry services

The first version of this business model should avoid complex logistics and payments. A simple referral flow is recommended before building a full marketplace.

Suggested initial flow:

1. User opens the laundry basket.
2. User taps an action such as `Send to laundry`.
3. App shows nearby or available partner laundry services.
4. User chooses a partner.
5. App opens WhatsApp, phone, website, or partner order link.
6. Partner confirms the service outside the app.
7. Referral is tracked through a referral code, link, or campaign identifier.

Partner laundry profiles may include:

- Business name
- Service area
- Contact options
- Website or order link
- WhatsApp or phone number
- Promo code
- Supported services
- Rating or user notes, later

The app can recommend partner services contextually when:

- The laundry basket has items
- Items need washing
- Items must be prepared for an upcoming event
- A user wants to send selected items to laundry

Recommendations should be optional and should not interfere with manual tracking. Users should be able to keep using the app fully without choosing a laundry partner.

Future marketplace features may include:

- In-app laundry orders
- Pickup and delivery scheduling
- Price estimates
- Order status tracking
- Payment
- Partner dashboard
- Ratings and reviews
- Repeat orders
- Event-preparation laundry orders

Laundry service integration should be built only after the core tracking workflow is useful on its own.

## Cloud Insights and Recommendation Partnerships

The app may eventually store user wardrobe and laundry data on a server to enable cloud sync, richer insights, and recommendation partnerships.

This should be treated as an opt-in feature. The initial app should not require login, but a future paid or premium version may offer cloud sync and cloud-based insights.

The product should position cloud storage as a user benefit:

- Sync data across devices
- Restore data if a phone is lost
- Generate wardrobe insights
- Help users avoid overbuying
- Recommend useful additions based on user-defined needs
- Provide relevant partner offers

The app should not position this as selling raw user data. Partner opportunities should be based on user intent, aggregated insights, consent-based recommendations, and anonymized analytics where appropriate.

Potential server-side insights:

- Wardrobe profile by category, color, brand, fabric, and season
- Most-used items
- Least-used items
- Cost-per-use trends
- Category usage frequency
- Seasonal wardrobe readiness
- Event readiness
- High-use items that may need replacement
- Laundry frequency and timing patterns
- User-defined wardrobe gaps

The app may allow users to define thresholds for their wardrobe.

Examples:

- At least 5 work shirts
- At least 3 gym shirts
- At least 2 formal dresses
- At least 7 underwear items
- At least 2 bed sheet sets
- At least 1 outfit suitable for weddings or formal events

When the user's collection is below a threshold, the app may recommend clothing pieces or services that fit the user's profile.

Example recommendation:

```text
Your work shirt collection is below your preferred minimum.
Based on your most-used colors and brands, here are options that match your wardrobe.
```

Recommendation signals may include:

- Category gaps
- User-defined thresholds
- Most-worn colors
- Preferred brands
- Common fabrics
- Seasonal needs
- Upcoming events
- Items with high usage
- Items frequently sent to laundry

Possible partnership categories:

- Clothing retailers
- Sustainable fashion brands
- Thrift and resale platforms
- Event outfit rental services
- Tailors and alteration services
- Shoe repair services
- Dry cleaners
- Laundry services
- Clothing care products
- Storage and organization products
- Subscription clothing boxes

Privacy and consent requirements:

- Cloud sync should be optional
- Users should clearly opt in to cloud insights
- Users should be able to delete their cloud data
- Users should control recommendation preferences
- Sensitive personal data should not be shared directly with partners
- Partner analytics should be aggregated or anonymized where possible
- Recommendations should be useful and contextual, not intrusive

Cloud and recommendation features should be considered after the local-first MVP proves useful.

## Cost Tracking

If purchase price is provided, the app should calculate cost per use.

Recommended formula:

```text
cost per use = purchase price / lifetime usage count
```

If lifetime usage count is 0, cost per use should not be shown or should be shown as unavailable.

## History

Each item should have a history timeline.

History events may include:

- Item created
- Item edited
- Usage recorded
- Usage deleted
- Added to laundry basket
- Removed from laundry basket
- Washed
- Archived
- Manual wash reason/comment
- Assigned to future event
- Prepared for future event

History is required so users can correct mistakes and understand item activity over time.

## Future Events

The app should allow users to create future events where specific clothing or washable items are planned for use.

Examples:

- Wedding party
- Work trip
- Vacation
- Formal dinner
- Graduation
- Holiday gathering
- User-defined event

Each event should include:

- Event name
- Event date
- Optional description
- Reminder timing, such as N days before the event
- One or more selected items planned for the event

The app should notify the user before the event so planned items can be sent to laundry or otherwise prepared in time.

When an event item is prepared or sent to laundry:

- The item should be added to the laundry basket, if appropriate
- The item should show an event-related badge or indicator
- The app should record the preparation action in history

When the item is marked as washed/prepared for the event:

- Item status should be reset to `Clean`
- Usage count since last wash should reset to 0
- Washing count should increase if the preparation involved washing
- Last washing date should update if the preparation involved washing
- The event preparation status for that item should be marked complete

The app should distinguish between:

- Planned for event
- Needs preparation
- Prepared for event

If the item is only reserved/prepared without washing, the app should allow marking the item as prepared without increasing washing count.

## Suggested Screens

### Items

Main list or grid of registered items.

Each item should show:

- Photo, if available
- Name
- Category
- Status badge
- Uses since last wash
- Needs washing indicator, when applicable

### Item Detail

Shows all item information and quick actions.

Suggested actions:

- Used today
- Add usage
- Add to laundry basket
- Mark washed
- Edit item
- Archive item

### Add/Edit Item

Form for entering item metadata and washing criteria.

The form should support entering an initial usage count greater than 0.

### Laundry Basket

Shows all items in the laundry basket and items that need washing.

Users can mark selected or all items as washed.

### History

Shows item activity over time, either per item or globally.

### Settings

Settings may include:

- Notification preferences
- Backup and restore
- Custom categories
- Custom fabrics
- Custom seasons
- Custom colors
- Default washing rules
- Default event reminder timing

## Recommended MVP

The first version should include:

- Add, edit, and delete washable items
- Support clothing, bedding, curtains, towels, and custom categories
- Optional photo field
- Purchase date
- Optional price
- Description
- Category, color, brand, fabric, and season
- Initial usage count greater than or equal to 0
- Usage count since last wash
- Lifetime usage count
- Washing count
- Last washing date
- Washing criteria by usage, date, usage-or-date, or manual
- Mark as used today
- Add usage for a custom date
- Undo/delete usage history entries
- Automatic `Needs washing` badge
- Laundry basket
- Add/remove items from laundry basket
- Mark one, many, or all basket items as washed
- Manual/out-of-cycle wash comment
- Item statuses
- Search by item name
- Filter/search by category
- Cost per use
- Future events with selected items
- Event reminders before the event date
- Event preparation status per item
- Local Room database

Features that can come after the MVP:

- Notifications
- Google Drive backup
- Laundry service partner directory and referral flow
- Advanced analytics
- Global history screen
- Additional filters
- Bulk editing
- Automatic scheduled backup

## Suggested Data Model

### Item

- id
- name
- categoryId
- colorId
- brand
- photoUri
- fabricId
- seasonId
- purchaseDate
- purchasePrice
- description
- usesSinceWash
- lifetimeUses
- washingCount
- lastWashingDate
- washingCriteriaType
- washingUsageThreshold
- washingDayThreshold
- status
- createdAt
- updatedAt
- archivedAt

### UsageEvent

- id
- itemId
- usedAt
- notes
- createdAt

### WashEvent

- id
- itemId
- washedAt
- usesAtTimeOfWash
- comment
- wasOutOfCycle
- createdAt

### LaundryBasketEntry

- id
- itemId
- addedAt
- reason
- comment

### FutureEvent

- id
- name
- eventDate
- description
- reminderDaysBefore
- createdAt
- updatedAt

### FutureEventItem

- id
- eventId
- itemId
- status
- addedAt
- preparedAt
- preparationComment
- preparationWashed

### Lookup Tables

Possible lookup/customization tables:

- Category
- Color
- Fabric
- Season

These should include predefined values and user-created values.
