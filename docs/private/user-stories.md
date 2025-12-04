# FairAir Complete User Stories

> A comprehensive catalog of all user stories, capabilities, and interactions available in the FairAir flight booking application.

---

## 1. Flight Search & Discovery

### 1.1 Search for Flights
**As a** traveler  
**I want to** search for available flights between two cities  
**So that** I can find and book a flight that fits my travel needs

**Acceptance Criteria:**
- I can select my departure city (origin)
- I can select my destination city (filtered based on valid routes from origin)
- I can select my departure date
- I can specify the number of passengers (adults, children, infants)
- I can initiate a search and see loading feedback
- I receive flight results with times, duration, and prices

---

### 1.2 Use Natural Language Search Interface
**As a** traveler  
**I want to** search for flights using a conversational sentence-builder  
**So that** booking feels intuitive and natural rather than filling out traditional forms

**Acceptance Criteria:**
- I see a sentence: "I want to fly from [Origin] to [Destination] departing on [Date] with [Passengers]"
- I can tap on highlighted words to open selection sheets
- Selected values appear inline in the sentence
- The launch button is disabled until I select a destination
- I can initiate search with a circular launch button

---

### 1.3 View Dynamic Destination Backgrounds
**As a** traveler  
**I want to** see beautiful destination imagery when I select where I'm going  
**So that** the app feels immersive and inspiring

**Acceptance Criteria:**
- Background changes based on my destination selection
- Images fade in/out smoothly when destination changes
- Default gradient background shows when no destination is selected

---

### 1.4 Filter Destinations by Origin
**As a** traveler  
**I want to** see only valid destinations when I select an origin  
**So that** I don't waste time selecting routes that don't exist

**Acceptance Criteria:**
- Destination list filters automatically when I select an origin
- Only airports with actual flight connections appear
- Invalid destinations are hidden

---

### 1.5 View No Results State
**As a** traveler  
**I want to** see a clear message when no flights are available  
**So that** I understand why I can't proceed and what alternatives I have

**Acceptance Criteria:**
- Friendly "No flights available" message displays
- Suggestion to try alternative dates
- Easy way to modify search criteria

---

### 1.6 View Prices on Date Grid
**As a** traveler  
**I want to** see flight prices displayed on each date in the date picker  
**So that** I can choose the date with the most affordable fare

**Acceptance Criteria:**
- Date picker shows a calendar/grid view
- Each date displays the lowest available fare for that date
- Dates with no flights are indicated differently (grayed out or marked)
- Price differences are visually apparent (color coding for cheap/expensive)
- Selecting a date updates the search with that date
- Loading indicator while fetching prices for visible dates
- Prices update when origin/destination changes

---

### 1.7 Select Trip Type
**As a** traveler  
**I want to** choose between one-way, round-trip, or multi-city flights  
**So that** I can book the type of journey I need

**Acceptance Criteria:**
- Trip type selector shows One-way, Round-trip, and Multi-city options
- Round-trip is selected by default
- Selecting round-trip enables return date selection
- Return date must be after departure date
- Search button requires return date for round-trip bookings
- Trip type persists in search state

---

### 1.8 Select Return Date for Round-Trip
**As a** traveler  
**I want to** select a return date when booking a round-trip  
**So that** I can book both outbound and return flights together

**Acceptance Criteria:**
- Return date field appears when round-trip is selected
- Return date picker shows dates starting from departure date
- Dates before departure date are disabled
- Low fare prices shown on return date calendar
- "Returning on [Date]" appears in the sentence builder
- Search includes both departure and return dates

---

## 2. Flight Results & Fare Selection

### 2.1 View Flight Search Results
**As a** traveler  
**I want to** see available flights with key information  
**So that** I can compare options and choose the best flight

**Acceptance Criteria:**
- Results appear in a visually appealing overlay
- Each flight card shows departure time, arrival time, origin/destination codes
- Flight duration is displayed
- Starting price is shown
- I can scroll through multiple results

---

### 2.2 Expand Flight Details
**As a** traveler  
**I want to** tap on a flight card to see more details  
**So that** I can learn more about the flight before selecting a fare

**Acceptance Criteria:**
- Tapping a flight card expands it
- Expanded card shows fare family options
- Only one card can be expanded at a time
- Tapping another card collapses the previous one

---

### 2.3 Compare Fare Families
**As a** traveler  
**I want to** compare different fare options (Fly, Fly+, FlyMax)  
**So that** I can choose the fare that matches my needs and budget

**Acceptance Criteria:**
- I see three fare options: Fly (basic), Fly+ (standard), FlyMax (premium)
- Each fare shows its price
- Each fare shows included perks (baggage, seat selection, flexibility)
- I can select any fare to continue booking

---

### 2.4 Select a Fare Family
**As a** traveler  
**I want to** select a specific fare option  
**So that** I can proceed with booking

**Acceptance Criteria:**
- I can tap on a fare option to select it
- Selected fare is visually highlighted
- I can proceed to passenger information
- Selected flight and fare information is preserved

---

### 2.5 Close Results and Modify Search
**As a** traveler  
**I want to** close the results overlay and return to search  
**So that** I can modify my search criteria

**Acceptance Criteria:**
- Close button is visible in results overlay
- Tapping close slides results away
- Search screen is revealed with previous criteria
- I can modify and search again

---

## 3. Passenger Information

### 3.1 Enter Passenger Details
**As a** traveler  
**I want to** enter information for each passenger  
**So that** the booking includes correct traveler data

**Acceptance Criteria:**
- Form sections appear for each passenger
- Required fields: Title, First Name, Last Name, Nationality, Date of Birth, Document ID
- Clear labels identify each field
- Passengers are labeled (Passenger 1, Passenger 2, etc.)

---

### 3.2 Validate Passenger Information
**As a** traveler  
**I want to** receive immediate feedback on form errors  
**So that** I can correct mistakes before submitting

**Acceptance Criteria:**
- Inline validation errors appear for invalid data
- Empty mandatory fields show error messages
- Names accept only letters
- Nationality accepts 2-letter country codes
- Date of birth validates year (1900-2100), month (01-12), day (based on month)
- Document ID is required
- Continue button is disabled until all fields are valid

---

### 3.3 Enter Contact Information
**As a** traveler  
**I want to** provide my email address  
**So that** I receive booking confirmation

**Acceptance Criteria:**
- Email field accepts valid email format
- Phone number field accepts digits and optional + prefix
- Contact info is required for booking

---

### 3.4 Navigate Between Passengers
**As a** traveler  
**I want to** easily navigate between passenger forms  
**So that** I can fill in details for all travelers

**Acceptance Criteria:**
- Scrollable list of passenger forms
- Visual separation between passengers
- Easy to identify which passenger I'm editing

---

## 4. Ancillary Services

### 4.1 View Available Add-ons
**As a** traveler  
**I want to** see optional extras I can add to my booking  
**So that** I can enhance my travel experience

**Acceptance Criteria:**
- Add-on options are clearly displayed
- Prices for each add-on are shown
- Current total is visible

---

### 4.2 Add Checked Baggage
**As a** traveler  
**I want to** add checked baggage to my booking  
**So that** I can bring more luggage on my trip

**Acceptance Criteria:**
- Toggle option for "Add Checked Bag"
- Price is clearly displayed (e.g., "+100 SAR")
- I can add baggage for specific passengers
- Total price updates immediately when toggled

---

### 4.3 Remove Ancillary Services
**As a** traveler  
**I want to** remove an add-on I previously selected  
**So that** I can adjust my booking before payment

**Acceptance Criteria:**
- I can toggle off any add-on
- Total price decreases when removed
- Selection state is clearly visible

---

### 4.4 Select Seats
**As a** traveler  
**I want to** choose specific seats for my flight  
**So that** I can sit where I prefer

**Acceptance Criteria:**
- Seat map is displayed for the aircraft
- Available seats are clearly marked
- Already taken seats are indicated
- I can select seats for each passenger
- Seat prices are shown (if applicable)

---

### 4.5 Pre-Order Meals
**As a** traveler  
**I want to** pre-order meals for my flight  
**So that** I can enjoy my preferred food during the journey

**Acceptance Criteria:**
- Menu categories displayed (Hot Meals, Cold Meals, Snacks, Beverages)
- Each meal shows name, description, price, and dietary tags
- I can select meals for each passenger
- Vegetarian, Halal, and other dietary options available
- Total meal cost added to booking total
- Meals can be added during booking or via Manage Booking

---

### 4.6 Purchase Extra Baggage
**As a** traveler  
**I want to** purchase additional baggage allowance  
**So that** I can bring more luggage on my trip

**Acceptance Criteria:**
- Baggage options displayed with weight limits (15kg, 23kg, 32kg)
- Prices shown for each option
- I can add baggage for each passenger individually
- Online price is cheaper than airport price
- Total updates when baggage added
- Can add up to 4 hours before departure

---

## 5. Payment

### 5.1 View Payment Summary
**As a** traveler  
**I want to** see a summary of my booking and total cost  
**So that** I know exactly what I'm paying for

**Acceptance Criteria:**
- Flight details are displayed
- Passenger summary is shown
- Ancillary services are listed
- Total amount is clearly visible
- Breakdown of costs is available

---

### 5.2 Enter Credit Card Details
**As a** traveler  
**I want to** enter my payment information  
**So that** I can complete my booking

**Acceptance Criteria:**
- Fields for: Card Number, Expiry Date, CVV, Cardholder Name
- Card number accepts only digits with auto-spacing (XXXX XXXX XXXX XXXX)
- Maximum 16 digits for card number
- Expiry date auto-formats (MM/YY)
- CVV accepts 3-4 digits
- Cardholder name accepts letters and spaces only

---

### 5.3 Validate Payment Information
**As a** traveler  
**I want to** receive immediate feedback on payment form errors  
**So that** I enter valid payment details

**Acceptance Criteria:**
- Card number validated (Luhn algorithm)
- Expiry date validated (not in the past)
- CVV length validated (3-4 digits)
- Cardholder name required
- Red border and error tooltip for invalid fields
- Pay button disabled until all fields are valid

---

### 5.4 Complete Payment
**As a** traveler  
**I want to** submit my payment and complete the booking  
**So that** I receive my booking confirmation

**Acceptance Criteria:**
- "Pay Now" button initiates payment
- Processing indicator shows during submission
- Button disables on first tap to prevent duplicate charges
- Success navigates to confirmation screen
- Failure shows clear error message with retry option

---

### 5.5 Handle Payment Errors
**As a** traveler  
**I want to** understand what went wrong if payment fails  
**So that** I can take corrective action

**Acceptance Criteria:**
- Clear error message explains the issue (e.g., "Card declined")
- Retry option is available
- My entered data is preserved
- I can modify payment details

---

## 6. Booking Confirmation

### 6.1 View Booking Confirmation
**As a** traveler  
**I want to** see my booking confirmation with PNR  
**So that** I have proof of my booking

**Acceptance Criteria:**
- PNR code is prominently displayed
- Flight details shown (route, date, time)
- Passenger names listed
- Payment receipt reference visible
- Formatted date and time display

---

### 6.2 Save Booking Locally
**As a** traveler  
**I want to** save my booking to my device  
**So that** I can access it offline or from the home screen

**Acceptance Criteria:**
- "Save to Home" or similar option available
- Booking stored locally
- Accessible from saved bookings section
- Available even without internet connection

---

### 6.3 Start New Booking
**As a** traveler  
**I want to** book another flight after confirmation  
**So that** I can plan additional trips

**Acceptance Criteria:**
- Option to start new search from confirmation screen
- Previous booking remains saved
- Search screen resets for new booking

---

## 7. Saved Bookings & Offline Access

### 7.1 View Saved Bookings
**As a** traveler  
**I want to** see all my saved booking confirmations  
**So that** I can reference them anytime

**Acceptance Criteria:**
- List of saved bookings displayed
- Each shows PNR, route, date
- I can tap to view full details
- Accessible from main navigation

---

### 7.2 Access Bookings Offline
**As a** traveler  
**I want to** view my saved bookings without internet  
**So that** I can access travel details in areas with poor connectivity

**Acceptance Criteria:**
- Saved bookings load from local storage
- Full booking details available offline
- Data integrity maintained
- Clear indication if viewing cached data

---

### 7.3 Delete Saved Booking
**As a** traveler  
**I want to** remove a booking from my saved list  
**So that** I can keep my list clean and relevant

**Acceptance Criteria:**
- Delete option for each saved booking
- Confirmation before deletion
- Booking removed from local storage

---

## 8. Language & Localization

### 8.1 Switch App Language
**As a** traveler  
**I want to** use the app in my preferred language  
**So that** I can understand all content easily

**Acceptance Criteria:**
- Language toggle accessible in settings/header
- English and Arabic supported
- All UI text changes to selected language
- Change persists across sessions

---

### 8.2 Use Arabic with RTL Layout
**As an** Arabic-speaking traveler  
**I want to** see proper right-to-left layout in Arabic  
**So that** the app feels natural to read and use

**Acceptance Criteria:**
- Layout flips to RTL for Arabic
- Text alignment follows RTL conventions
- Navigation and scrolling respect RTL direction
- Arabic fonts (Noto Kufi Arabic) applied
- Search sentence reads naturally in Arabic

---

### 8.3 Switch Language Mid-Session
**As a** traveler  
**I want to** change language without losing my progress  
**So that** I can switch whenever convenient

**Acceptance Criteria:**
- Current navigation state preserved
- Any entered data retained
- Language change is instant
- No visible layout jumps or text overflow

---

## 9. App Settings

### 9.1 Access App Settings
**As a** traveler  
**I want to** access app settings  
**So that** I can customize my experience

**Acceptance Criteria:**
- Settings accessible from navigation
- Language preference option
- Clear, organized settings layout

---

### 9.2 Configure Preferences
**As a** traveler  
**I want to** set my preferences  
**So that** the app works the way I like

**Acceptance Criteria:**
- Language selection
- Preferences persist between sessions
- Easy to reset to defaults

---

## 10. Navigation & General UX

### 10.1 Navigate with Back Button (Android)
**As an** Android user  
**I want to** use the system back button  
**So that** navigation feels native

**Acceptance Criteria:**
- Back button integrates with app navigation stack
- Goes to previous screen, not exit app (unless at root)
- Consistent behavior throughout app

---

### 10.2 View Content in Safe Areas (iOS)
**As an** iPhone user  
**I want to** see content properly around the notch and home indicator  
**So that** nothing is cut off or obscured

**Acceptance Criteria:**
- Content respects iOS safe area insets
- No overlap with notch area
- Home indicator area respected

---

### 10.3 Use App on Web (WASM)
**As a** web user  
**I want to** use the full booking experience in a browser  
**So that** I can book flights from any device

**Acceptance Criteria:**
- Full functionality available on web
- Responsive design for various screen sizes
- Keyboard navigation supported
- Same features as mobile apps

---

### 10.4 Navigate Between Main Sections
**As a** traveler  
**I want to** easily access main sections of the app  
**So that** I can find what I need quickly

**Acceptance Criteria:**
- Clear navigation to Search, Saved Bookings, Settings
- Current section is indicated
- Smooth transitions between sections

---

### 10.5 View Loading States
**As a** traveler  
**I want to** see feedback when the app is loading  
**So that** I know something is happening

**Acceptance Criteria:**
- Loading skeletons/shimmers during data fetch
- Progress indicators for long operations
- Engagement maintained during waits
- Smooth transition from loading to content

---

## 11. Error Handling & Recovery

### 11.1 Handle Network Errors
**As a** traveler  
**I want to** understand when network issues occur  
**So that** I know it's not a problem with my booking

**Acceptance Criteria:**
- Clear error messages for network failures
- Retry options available
- Cached data remains accessible
- App doesn't crash on network errors

---

### 11.2 Handle Session Expiration
**As a** traveler  
**I want to** continue my booking if my session expires  
**So that** I don't lose my entered data

**Acceptance Criteria:**
- Entered data preserved locally
- Re-authentication prompt (if applicable)
- Progress not lost
- Clear explanation of what happened

---

### 11.3 Handle Unavailable Flights
**As a** traveler  
**I want to** know if my selected flight is no longer available  
**So that** I can choose an alternative

**Acceptance Criteria:**
- Clear error message when flight unavailable
- Directed back to search
- Explanation that flight is no longer available
- Easy to search again

---

### 11.4 Handle Malformed Data
**As a** traveler  
**I want to** the app to handle unexpected data gracefully  
**So that** I can still use the app

**Acceptance Criteria:**
- Generic error screen with retry option
- App doesn't crash
- Error logged for debugging
- Graceful degradation

---

## 12. Guest Checkout & Authentication

### 12.1 Book as Guest
**As a** traveler  
**I want to** complete a booking without creating an account  
**So that** I can book quickly

**Acceptance Criteria:**
- No login required for full booking flow
- Only email required for confirmation delivery
- Full functionality available to guests

---

### 12.2 Create Account After Booking
**As a** traveler  
**I want to** optionally create an account after booking  
**So that** I can save my PNR for future access

**Acceptance Criteria:**
- Optional account creation offered post-booking
- PNR can be saved to account
- Future bookings can be linked
- Not required to complete booking

---

### 12.3 Login to Existing Account
**As a** returning traveler  
**I want to** login to my account  
**So that** I can access my saved bookings

**Acceptance Criteria:**
- Login option available
- Access to saved bookings after login
- Preferences restored
- Secure authentication

---

## 13. App Initialization

### 13.1 Launch App
**As a** traveler  
**I want to** open the app and start using it quickly  
**So that** I can begin my booking without waiting

**Acceptance Criteria:**
- App launches and displays search in under 3 seconds
- Route network fetched and cached
- Search screen functional immediately
- Background data refresh if needed

---

### 13.2 Use Cached Data
**As a** returning traveler  
**I want to** the app to load immediately using cached data  
**So that** I don't wait for network every time

**Acceptance Criteria:**
- Search screen loads using cached routes
- No network required for initial display
- Background refresh updates data silently
- Stale data replaced when fresh data arrives

---

### 13.3 Benefit from Route Search Caching
**As a** traveler  
**I want to** flight search results to be cached on the server  
**So that** repeated searches for the same route return faster

**Acceptance Criteria:**
- Flight search results are cached by route (origin + destination + date + passenger count)
- Cache has a reasonable TTL (e.g., 5 minutes) to balance freshness and performance
- Subsequent searches for the same route return instantly from cache
- Cache invalidates after TTL expires to ensure fresh pricing
- Works transparently without user intervention

---

## 14. Visual Design & Experience

### 14.1 Experience Modern Glassmorphic Design
**As a** traveler  
**I want to** use a visually stunning, modern interface  
**So that** booking feels premium and enjoyable

**Acceptance Criteria:**
- Deep purple/black background theme
- Glassmorphism effects on cards (blur, transparency)
- Neon lime accent color for CTAs
- Smooth animations and transitions
- Consistent design language throughout

---

### 14.2 View Flight Route Visualization
**As a** traveler  
**I want to** see a visual representation of my flight route  
**So that** the journey feels tangible

**Acceptance Criteria:**
- Visual route line on flight cards
- Origin and destination codes displayed
- Animated accent dot on route line
- Clear visual hierarchy

---

## 15. Online Check-In

### 15.1 Initiate Online Check-In
**As a** traveler  
**I want to** check in online before my flight  
**So that** I can skip airport check-in queues

**Acceptance Criteria:**
- Check-in accessible from landing page and navigation
- Enter PNR (booking reference) and last name
- Check-in available 24-48 hours before departure
- Clear error if outside check-in window

---

### 15.2 Select Passengers for Check-In
**As a** traveler  
**I want to** choose which passengers to check in  
**So that** I can check in for all or some travelers

**Acceptance Criteria:**
- List of passengers on the booking displayed
- Each passenger has a checkbox for selection
- "Select All" option available
- At least one passenger must be selected
- Continue button enabled when selection made

---

### 15.3 View and Download Boarding Pass
**As a** traveler  
**I want to** receive my boarding pass after check-in  
**So that** I have proof of check-in for airport security

**Acceptance Criteria:**
- Boarding pass displayed with passenger name, flight details
- QR/barcode for scanning at airport
- Seat number shown if assigned
- Option to save/download boarding pass
- Can be viewed offline after download

---

### 15.4 Check-In Multiple Passengers
**As a** traveler  
**I want to** check in multiple passengers at once  
**So that** I can complete the process for my group efficiently

**Acceptance Criteria:**
- Select multiple passengers from the list
- Single confirmation for all selected passengers
- Individual boarding passes generated for each
- All boarding passes accessible from confirmation

---

### 15.5 Handle Check-In Restrictions
**As a** traveler  
**I want to** understand why I cannot check in online  
**So that** I know what to do at the airport

**Acceptance Criteria:**
- Clear message if check-in not available
- Reasons explained (too early, too late, documentation required)
- Instructions for airport check-in
- Contact information for assistance

---

## 16. Manage Booking

### 16.1 Retrieve Existing Booking
**As a** traveler  
**I want to** access my existing booking using PNR and last name  
**So that** I can view or modify my reservation

**Acceptance Criteria:**
- Enter PNR (booking reference) and last name
- Booking details retrieved and displayed
- Flight information, passengers, and services shown
- Clear error if booking not found

---

### 16.2 View Booking Details
**As a** traveler  
**I want to** see complete details of my booking  
**So that** I can verify all information is correct

**Acceptance Criteria:**
- Flight details (route, date, time, flight number)
- Passenger list with names and documents
- Selected fare family and included services
- Ancillary services (baggage, meals, seats)
- Total paid and payment status

---

### 16.3 Modify Flight Date or Time
**As a** traveler  
**I want to** change my flight to a different date or time  
**So that** I can adjust my travel plans

**Acceptance Criteria:**
- "Change Flight" option available if fare allows
- See available alternatives for same route
- Price difference displayed (if any)
- Modification fee shown based on fare rules
- Confirm changes and pay difference if required

---

### 16.4 Update Contact Information
**As a** traveler  
**I want to** update my email or phone number  
**So that** I receive important flight notifications

**Acceptance Criteria:**
- Edit contact information option
- Email and phone fields editable
- Validation on new values
- Confirmation of update
- No fee for contact changes

---

### 16.5 Cancel Booking
**As a** traveler  
**I want to** cancel my booking  
**So that** I can receive a refund if eligible

**Acceptance Criteria:**
- Cancel option available if fare allows
- Cancellation policy clearly explained
- Refund amount or credit shown
- Reason for cancellation (optional)
- Confirmation before final cancellation
- Refund processed to original payment method

---

### 16.6 Add Services to Existing Booking
**As a** traveler  
**I want to** add extra services after booking  
**So that** I can enhance my trip without rebooking

**Acceptance Criteria:**
- "Add Services" option in manage booking
- Baggage, meals, seats available for purchase
- Prices shown for each service
- Add for specific passengers
- Payment for additional services
- Updated booking confirmation

---

## 17. Membership (Adeal)

### 17.1 View Membership Plans
**As a** frequent traveler  
**I want to** see available membership plans  
**So that** I can evaluate subscription benefits

**Acceptance Criteria:**
- Three plans displayed: 12, 24, 36 round trips per year
- Monthly price for each plan shown
- Benefits listed (domestic flights, booking window, included bag)
- "Recommended" badge on popular plan
- Comparison of plans easy to understand

---

### 17.2 Subscribe to Membership
**As a** frequent traveler  
**I want to** subscribe to a membership plan  
**So that** I can fly at a fixed monthly price

**Acceptance Criteria:**
- Select desired plan
- Account creation if not logged in
- Payment method entry (credit card for monthly billing)
- Terms and conditions acceptance
- Subscription confirmation with start date
- Welcome email with membership details

---

### 17.3 Book Flights with Membership
**As a** member  
**I want to** book flights using my membership  
**So that** I can travel without paying per-flight

**Acceptance Criteria:**
- Member login shows available trips
- Domestic destinations only (as per plan)
- Book 3+ days before departure
- Remaining trips for current period shown
- No additional flight cost (base fare)
- Add-ons available for purchase separately

---

### 17.4 View Membership Usage
**As a** member  
**I want to** see how many trips I've used  
**So that** I can plan my remaining travel

**Acceptance Criteria:**
- Usage dashboard in membership section
- Trips used vs. trips remaining
- Current billing period dates
- History of booked flights
- Next billing date and amount

---

### 17.5 Manage Subscription
**As a** member  
**I want to** manage my subscription settings  
**So that** I can control my membership

**Acceptance Criteria:**
- View current plan details
- Toggle auto-renewal on/off
- Update payment method
- View billing history
- Cancel subscription option

---

### 17.6 Cancel Membership
**As a** member  
**I want to** cancel my membership  
**So that** I can stop future charges

**Acceptance Criteria:**
- Cancel option in subscription settings
- Cancellation policy explained (12-month commitment)
- Early cancellation fees shown if applicable
- Confirmation required before cancellation
- Membership valid until end of current billing period

---

## 18. Help & Support

### 18.1 Browse Help Categories
**As a** traveler  
**I want to** browse help topics by category  
**So that** I can find answers to my questions

**Acceptance Criteria:**
- Help center accessible from navigation
- Categories displayed: Booking, Baggage, Flight, Special Assistance, App/Website, Membership
- Each category shows article count
- Tap to expand category and see articles
- Visual icons for each category

---

### 18.2 Read FAQ Articles
**As a** traveler  
**I want to** read answers to common questions  
**So that** I can resolve issues without contacting support

**Acceptance Criteria:**
- FAQ accordion with question titles
- Tap to expand and see answer
- Clear, helpful answers
- Related articles suggested
- Search functionality to find specific topics

---

### 18.3 Contact Customer Support
**As a** traveler  
**I want to** contact support when I need help  
**So that** I can resolve complex issues

**Acceptance Criteria:**
- Contact options displayed (phone, email)
- Phone number for 24/7 support
- Email support link
- Expected response times shown
- Social media links for additional support

---

### 18.4 Access Help from Any Screen
**As a** traveler  
**I want to** access help easily from anywhere in the app  
**So that** I can get assistance when I'm stuck

**Acceptance Criteria:**
- Help icon/link accessible in navigation
- Quick actions (Call, Email) prominent
- Contextual help for complex screens
- Search bar for quick lookups

---

## 19. External Services

### 19.1 Book Hotels via Partner
**As a** traveler  
**I want to** book hotels through the app  
**So that** I can arrange accommodation for my trip

**Acceptance Criteria:**
- Hotels service card on landing page
- Links to Booking.com partner
- Pre-filled destination if available
- Opens in new browser tab/window
- Partner attribution shown

---

### 19.2 Rent a Car via Partner
**As a** traveler  
**I want to** rent a car through the app  
**So that** I can arrange ground transportation

**Acceptance Criteria:**
- Car Rental service card on landing page
- Links to Rentalcars.com partner
- Pre-filled pickup location if available
- Opens in new browser tab/window
- Partner attribution shown

---

### 19.3 Access External Links Correctly
**As a** traveler  
**I want to** external service links to open properly  
**So that** I can complete bookings with partners

**Acceptance Criteria:**
- Links open in external browser (mobile)
- Links open in new tab (web)
- Phone links open dialer
- Email links open mail client
- No broken links or errors

---

## 20. Landing Page

### 20.1 View Landing Page
**As a** visitor  
**I want to** see an engaging landing page  
**So that** I understand what FairAir offers

**Acceptance Criteria:**
- Hero section with main CTA ("Search Flights")
- Quick services section (Check-in, Manage, Membership)
- External services (Hotels, Car Rental, Help)
- Promotional deals section
- Popular destinations section
- Feature highlights (Best Price, Flexible, Support)

---

### 20.2 Access Quick Services from Landing
**As a** traveler  
**I want to** access key services from the landing page  
**So that** I can quickly get to what I need

**Acceptance Criteria:**
- Check-In card links to check-in flow
- Manage Booking card links to manage flow
- Membership card links to membership section
- Hotels card opens external hotel booking
- Car Rental card opens external car rental
- Help card links to help center

---

### 20.3 View Promotional Deals
**As a** traveler  
**I want to** see current flight deals and promotions  
**So that** I can find discounted fares

**Acceptance Criteria:**
- Deal cards with origin/destination, price, dates
- Badge for deal type (Flash Sale, % Off, New Route)
- Tap deal to pre-fill search with those cities
- Visual appeal with colors and formatting

---

### 20.4 Explore Popular Destinations
**As a** traveler  
**I want to** see popular destinations  
**So that** I can discover where to travel

**Acceptance Criteria:**
- Destination cards with city name and country
- Airport code displayed
- Tap to pre-fill destination in search
- Horizontal scroll for multiple destinations

---

---

## Summary Statistics

| Category | User Stories |
|----------|--------------|
| Flight Search & Discovery | 8 |
| Flight Results & Fare Selection | 5 |
| Passenger Information | 4 |
| Ancillary Services | 6 |
| Payment | 5 |
| Booking Confirmation | 3 |
| Saved Bookings & Offline | 3 |
| Language & Localization | 3 |
| App Settings | 2 |
| Navigation & General UX | 5 |
| Error Handling & Recovery | 4 |
| Guest Checkout & Auth | 3 |
| App Initialization | 3 |
| Visual Design | 2 |
| Online Check-In | 5 |
| Manage Booking | 6 |
| Membership (Adeal) | 6 |
| Help & Support | 4 |
| External Services | 3 |
| Landing Page | 4 |
| **Total** | **84** |

---

*Last Updated: December 4, 2025*
