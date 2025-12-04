package com.fairair.app.localization

/**
 * Interface defining all translatable strings in the app.
 */
interface AppStrings {
    // General
    val appName: String
    val loading: String
    val error: String
    val retry: String
    val cancel: String
    val continue_: String
    val back: String
    val done: String
    val save: String
    val delete: String
    val confirm: String
    val close: String

    // Search Screen
    val searchTitle: String
    val selectOrigin: String
    val selectDestination: String
    val selectDate: String
    val departureDate: String
    val passengers: String
    val adults: String
    val children: String
    val infants: String
    val searchFlights: String
    val noRoutesAvailable: String

    // Results Screen
    val availableFlights: String
    val noFlightsAvailable: String
    val tryDifferentDate: String
    val selectFare: String
    val flightDetails: String
    val duration: String
    val direct: String

    // Fare Families
    val fly: String
    val flyPlus: String
    val flyMax: String
    val carryOn: String
    val checkedBag: String
    val seatSelection: String
    val priorityBoarding: String
    val loungeAccess: String

    // Passengers Screen
    val passengerInfo: String
    val passengerDetails: String
    val title: String
    val firstName: String
    val lastName: String
    val dateOfBirth: String
    val nationality: String
    val documentType: String
    val documentNumber: String
    val documentExpiry: String
    val email: String
    val phone: String
    val mr: String
    val mrs: String
    val ms: String
    val miss: String
    val passport: String
    val nationalId: String

    // Ancillaries Screen
    val extras: String
    val checkedBaggage: String
    val inFlightMeals: String
    val skipExtras: String
    val addLater: String
    val noBaggage: String
    val noMeal: String

    // Payment Screen
    val payment: String
    val paymentMethod: String
    val cardNumber: String
    val cardholderName: String
    val expiryDate: String
    val cvv: String
    val securePayment: String
    val payNow: String
    val processing: String
    val totalToPay: String
    val orderSummary: String
    val flightPrice: String
    val extrasPrice: String
    val total: String

    // Confirmation Screen
    val bookingConfirmed: String
    val bookingReference: String
    val pnr: String
    val bookingSuccessMessage: String
    val viewETicket: String
    val bookAnother: String
    val emailSentMessage: String
    val checkInReminder: String
    val passengerCount: String
    val totalPaid: String

    // Saved Bookings
    val savedBookings: String
    val noSavedBookings: String
    val savedBookingsDescription: String
    val removeBooking: String
    val removeBookingConfirm: String

    // Settings
    val settings: String
    val language: String
    val english: String
    val arabic: String
    val changeLanguage: String

    // Validation Messages
    val fieldRequired: String
    val invalidEmail: String
    val invalidPhone: String
    val invalidCardNumber: String
    val invalidExpiry: String
    val invalidCvv: String
    val nameTooShort: String
    val nameTooLong: String

    // Error Messages
    val networkError: String
    val serverError: String
    val unknownError: String
    val sessionExpired: String

    // Velocity UI - Sentence Builder
    val velocitySentencePrefix: String
    val velocitySentenceFrom: String
    val velocitySentenceTo: String
    val velocitySentenceDeparting: String
    val velocitySentenceReturning: String
    val velocitySentenceWith: String
    val velocitySelectOrigin: String
    val velocitySelectDestination: String
    val velocitySelectDate: String
    val velocitySelectReturnDate: String
    val velocityAdult: String
    val velocityAdults: String
    val velocityOneWay: String
    val velocityRoundTrip: String
    val velocityMultiCity: String
    val velocityTripType: String

    // Velocity UI - Results
    val velocityResultsTitle: String
    val velocityFromPrice: String
    val velocityNoResults: String
    val velocityNoResultsHint: String
    val velocitySearching: String

    // Landing Page
    val landingHeroTitle1: String
    val landingHeroTitle2: String
    val landingHeroSubtitle: String
    val landingSearchFlights: String
    val landingFaresFrom: String
    val landingSignIn: String
    val landingSpecialOffers: String
    val landingSpecialOffersSubtitle: String
    val landingPopularDestinations: String
    val landingPopularDestinationsSubtitle: String
    val landingWhyUs: String
    val landingWhyUsSubtitle: String
    val landingBestPrice: String
    val landingBestPriceDesc: String
    val landingFlexibleBooking: String
    val landingFlexibleBookingDesc: String
    val landingSupport: String
    val landingSupportDesc: String
    val landingRewards: String
    val landingRewardsDesc: String
    val landingFlashSale: String
    val landingPercentOff: String
    val landingNewRoute: String
    val landingPopular: String
    val landingFrom: String
    val landingFooterAbout: String
    val landingFooterHelp: String
    val landingFooterPrivacy: String
    val landingFooterTerms: String
    val landingFooterCopyright: String
    val landingFooterPortfolio: String
    val landingSignOut: String
    val landingMyBookings: String
    val landingWelcome: String
}

/**
 * English strings implementation.
 */
object EnglishStrings : AppStrings {
    // General
    override val appName = "FairAir"
    override val loading = "Loading..."
    override val error = "Error"
    override val retry = "Retry"
    override val cancel = "Cancel"
    override val continue_ = "Continue"
    override val back = "Back"
    override val done = "Done"
    override val save = "Save"
    override val delete = "Delete"
    override val confirm = "Confirm"
    override val close = "Close"

    // Search Screen
    override val searchTitle = "Search Flights"
    override val selectOrigin = "Where from?"
    override val selectDestination = "Where to?"
    override val selectDate = "Select Date"
    override val departureDate = "Departure Date"
    override val passengers = "Passengers"
    override val adults = "Adults"
    override val children = "Children"
    override val infants = "Infants"
    override val searchFlights = "Search Flights"
    override val noRoutesAvailable = "No routes available"

    // Results Screen
    override val availableFlights = "Available Flights"
    override val noFlightsAvailable = "No flights available"
    override val tryDifferentDate = "Try searching for a different date or route"
    override val selectFare = "Select a fare"
    override val flightDetails = "Flight Details"
    override val duration = "Duration"
    override val direct = "Direct"

    // Fare Families
    override val fly = "Fly"
    override val flyPlus = "Fly+"
    override val flyMax = "FlyMax"
    override val carryOn = "Carry-on"
    override val checkedBag = "Checked bag"
    override val seatSelection = "Seat selection"
    override val priorityBoarding = "Priority boarding"
    override val loungeAccess = "Lounge access"

    // Passengers Screen
    override val passengerInfo = "Passenger Information"
    override val passengerDetails = "Passenger Details"
    override val title = "Title"
    override val firstName = "First Name"
    override val lastName = "Last Name"
    override val dateOfBirth = "Date of Birth"
    override val nationality = "Nationality"
    override val documentType = "Document Type"
    override val documentNumber = "Document Number"
    override val documentExpiry = "Document Expiry"
    override val email = "Email"
    override val phone = "Phone"
    override val mr = "Mr"
    override val mrs = "Mrs"
    override val ms = "Ms"
    override val miss = "Miss"
    override val passport = "Passport"
    override val nationalId = "National ID"

    // Ancillaries Screen
    override val extras = "Extras"
    override val checkedBaggage = "Checked Baggage"
    override val inFlightMeals = "In-flight Meals"
    override val skipExtras = "Skip extras?"
    override val addLater = "You can add these later from Manage Booking"
    override val noBaggage = "No checked baggage"
    override val noMeal = "No meal"

    // Payment Screen
    override val payment = "Payment"
    override val paymentMethod = "Payment Method"
    override val cardNumber = "Card Number"
    override val cardholderName = "Cardholder Name"
    override val expiryDate = "Expiry (MM/YY)"
    override val cvv = "CVV"
    override val securePayment = "Secure Payment"
    override val payNow = "Pay Now"
    override val processing = "Processing..."
    override val totalToPay = "Total to pay"
    override val orderSummary = "Order Summary"
    override val flightPrice = "Flight"
    override val extrasPrice = "Extras"
    override val total = "Total"

    // Confirmation Screen
    override val bookingConfirmed = "Booking Confirmed!"
    override val bookingReference = "Booking Reference (PNR)"
    override val pnr = "PNR"
    override val bookingSuccessMessage = "Your flight has been successfully booked"
    override val viewETicket = "View E-Ticket"
    override val bookAnother = "Book Another Flight"
    override val emailSentMessage = "A confirmation email with your e-ticket has been sent to your registered email address."
    override val checkInReminder = "Please check in online 24 hours before departure."
    override val passengerCount = "Passengers"
    override val totalPaid = "Total Paid"

    // Saved Bookings
    override val savedBookings = "Saved Bookings"
    override val noSavedBookings = "No saved bookings"
    override val savedBookingsDescription = "Your completed bookings will appear here for offline access"
    override val removeBooking = "Remove"
    override val removeBookingConfirm = "This will remove the booking from your saved list. You can always re-fetch it from the server using the PNR."

    // Settings
    override val settings = "Settings"
    override val language = "Language"
    override val english = "English"
    override val arabic = "Arabic"
    override val changeLanguage = "Change Language"

    // Validation Messages
    override val fieldRequired = "This field is required"
    override val invalidEmail = "Invalid email address"
    override val invalidPhone = "Invalid phone number"
    override val invalidCardNumber = "Invalid card number"
    override val invalidExpiry = "Invalid expiry date"
    override val invalidCvv = "Invalid CVV"
    override val nameTooShort = "Name must be at least 2 characters"
    override val nameTooLong = "Name must be less than 50 characters"

    // Error Messages
    override val networkError = "Network error. Please check your connection."
    override val serverError = "Server error. Please try again later."
    override val unknownError = "An unexpected error occurred."
    override val sessionExpired = "Session expired. Please search again."

    // Velocity UI - Sentence Builder
    override val velocitySentencePrefix = "I want to fly"
    override val velocitySentenceFrom = "from"
    override val velocitySentenceTo = "to"
    override val velocitySentenceDeparting = "departing on"
    override val velocitySentenceReturning = "returning on"
    override val velocitySentenceWith = "with"
    override val velocitySelectOrigin = "Origin"
    override val velocitySelectDestination = "Destination"
    override val velocitySelectDate = "Date"
    override val velocitySelectReturnDate = "Return"
    override val velocityAdult = "Adult"
    override val velocityAdults = "Adults"
    override val velocityOneWay = "One-way"
    override val velocityRoundTrip = "Round trip"
    override val velocityMultiCity = "Multi-city"
    override val velocityTripType = "Trip type"

    // Velocity UI - Results
    override val velocityResultsTitle = "Available Flights"
    override val velocityFromPrice = "from"
    override val velocityNoResults = "No flights found"
    override val velocityNoResultsHint = "Try a different date or route"
    override val velocitySearching = "Searching flights..."

    // Landing Page
    override val landingHeroTitle1 = "Travel made simple."
    override val landingHeroTitle2 = "Book your next adventure"
    override val landingHeroSubtitle = "Discover incredible destinations across the Middle East and beyond with award-winning service and the best prices."
    override val landingSearchFlights = "Search Flights"
    override val landingFaresFrom = "Fares starting from SAR 199"
    override val landingSignIn = "Sign In"
    override val landingSpecialOffers = "Special Offers"
    override val landingSpecialOffersSubtitle = "Limited time deals on popular routes"
    override val landingPopularDestinations = "Popular Destinations"
    override val landingPopularDestinationsSubtitle = "Where travelers are heading"
    override val landingWhyUs = "Why FairAir?"
    override val landingWhyUsSubtitle = "Travel with confidence"
    override val landingBestPrice = "Best Price Guarantee"
    override val landingBestPriceDesc = "Find a lower price? We'll match it."
    override val landingFlexibleBooking = "Flexible Booking"
    override val landingFlexibleBookingDesc = "Free changes up to 24h before departure."
    override val landingSupport = "24/7 Support"
    override val landingSupportDesc = "Our team is always here to help."
    override val landingRewards = "Earn Rewards"
    override val landingRewardsDesc = "Points on every flight you book."
    override val landingFlashSale = "FLASH SALE"
    override val landingPercentOff = "20% OFF"
    override val landingNewRoute = "NEW ROUTE"
    override val landingPopular = "POPULAR"
    override val landingFrom = "from"
    override val landingFooterAbout = "About"
    override val landingFooterHelp = "Help"
    override val landingFooterPrivacy = "Privacy"
    override val landingFooterTerms = "Terms"
    override val landingFooterCopyright = "© 2024 FairAir. All rights reserved."
    override val landingFooterPortfolio = "A portfolio project"
    override val landingSignOut = "Sign Out"
    override val landingMyBookings = "My Bookings"
    override val landingWelcome = "Welcome"
}

/**
 * Arabic strings implementation.
 */
object ArabicStrings : AppStrings {
    // General
    override val appName = "فير إير"
    override val loading = "جاري التحميل..."
    override val error = "خطأ"
    override val retry = "إعادة المحاولة"
    override val cancel = "إلغاء"
    override val continue_ = "متابعة"
    override val back = "رجوع"
    override val done = "تم"
    override val save = "حفظ"
    override val delete = "حذف"
    override val confirm = "تأكيد"
    override val close = "إغلاق"

    // Search Screen
    override val searchTitle = "البحث عن رحلات"
    override val selectOrigin = "من أين؟"
    override val selectDestination = "إلى أين؟"
    override val selectDate = "اختر التاريخ"
    override val departureDate = "تاريخ المغادرة"
    override val passengers = "المسافرون"
    override val adults = "بالغون"
    override val children = "أطفال"
    override val infants = "رضع"
    override val searchFlights = "البحث عن رحلات"
    override val noRoutesAvailable = "لا توجد مسارات متاحة"

    // Results Screen
    override val availableFlights = "الرحلات المتاحة"
    override val noFlightsAvailable = "لا توجد رحلات متاحة"
    override val tryDifferentDate = "جرب البحث بتاريخ أو مسار مختلف"
    override val selectFare = "اختر السعر"
    override val flightDetails = "تفاصيل الرحلة"
    override val duration = "المدة"
    override val direct = "مباشر"

    // Fare Families
    override val fly = "فلاي"
    override val flyPlus = "فلاي+"
    override val flyMax = "فلاي ماكس"
    override val carryOn = "حقيبة يد"
    override val checkedBag = "حقيبة مسجلة"
    override val seatSelection = "اختيار المقعد"
    override val priorityBoarding = "أولوية الصعود"
    override val loungeAccess = "دخول الصالة"

    // Passengers Screen
    override val passengerInfo = "معلومات المسافر"
    override val passengerDetails = "تفاصيل المسافر"
    override val title = "اللقب"
    override val firstName = "الاسم الأول"
    override val lastName = "اسم العائلة"
    override val dateOfBirth = "تاريخ الميلاد"
    override val nationality = "الجنسية"
    override val documentType = "نوع الوثيقة"
    override val documentNumber = "رقم الوثيقة"
    override val documentExpiry = "تاريخ انتهاء الوثيقة"
    override val email = "البريد الإلكتروني"
    override val phone = "الهاتف"
    override val mr = "السيد"
    override val mrs = "السيدة"
    override val ms = "الآنسة"
    override val miss = "الآنسة"
    override val passport = "جواز السفر"
    override val nationalId = "الهوية الوطنية"

    // Ancillaries Screen
    override val extras = "الإضافات"
    override val checkedBaggage = "الأمتعة المسجلة"
    override val inFlightMeals = "وجبات الطائرة"
    override val skipExtras = "تخطي الإضافات؟"
    override val addLater = "يمكنك إضافتها لاحقاً من إدارة الحجز"
    override val noBaggage = "بدون أمتعة مسجلة"
    override val noMeal = "بدون وجبة"

    // Payment Screen
    override val payment = "الدفع"
    override val paymentMethod = "طريقة الدفع"
    override val cardNumber = "رقم البطاقة"
    override val cardholderName = "اسم حامل البطاقة"
    override val expiryDate = "تاريخ الانتهاء"
    override val cvv = "رمز التحقق"
    override val securePayment = "دفع آمن"
    override val payNow = "ادفع الآن"
    override val processing = "جاري المعالجة..."
    override val totalToPay = "المبلغ الإجمالي"
    override val orderSummary = "ملخص الطلب"
    override val flightPrice = "الرحلة"
    override val extrasPrice = "الإضافات"
    override val total = "الإجمالي"

    // Confirmation Screen
    override val bookingConfirmed = "تم تأكيد الحجز!"
    override val bookingReference = "رقم الحجز"
    override val pnr = "رقم الحجز"
    override val bookingSuccessMessage = "تم حجز رحلتك بنجاح"
    override val viewETicket = "عرض التذكرة الإلكترونية"
    override val bookAnother = "حجز رحلة أخرى"
    override val emailSentMessage = "تم إرسال رسالة تأكيد بالتذكرة الإلكترونية إلى بريدك الإلكتروني المسجل."
    override val checkInReminder = "يرجى إنهاء إجراءات السفر عبر الإنترنت قبل 24 ساعة من المغادرة."
    override val passengerCount = "المسافرون"
    override val totalPaid = "المبلغ المدفوع"

    // Saved Bookings
    override val savedBookings = "الحجوزات المحفوظة"
    override val noSavedBookings = "لا توجد حجوزات محفوظة"
    override val savedBookingsDescription = "ستظهر حجوزاتك المكتملة هنا للوصول إليها دون اتصال"
    override val removeBooking = "إزالة"
    override val removeBookingConfirm = "سيتم إزالة الحجز من قائمتك المحفوظة. يمكنك دائماً استرجاعه من الخادم باستخدام رقم الحجز."

    // Settings
    override val settings = "الإعدادات"
    override val language = "اللغة"
    override val english = "الإنجليزية"
    override val arabic = "العربية"
    override val changeLanguage = "تغيير اللغة"

    // Validation Messages
    override val fieldRequired = "هذا الحقل مطلوب"
    override val invalidEmail = "عنوان بريد إلكتروني غير صالح"
    override val invalidPhone = "رقم هاتف غير صالح"
    override val invalidCardNumber = "رقم بطاقة غير صالح"
    override val invalidExpiry = "تاريخ انتهاء غير صالح"
    override val invalidCvv = "رمز تحقق غير صالح"
    override val nameTooShort = "يجب أن يكون الاسم حرفين على الأقل"
    override val nameTooLong = "يجب أن يكون الاسم أقل من 50 حرفاً"

    // Error Messages
    override val networkError = "خطأ في الشبكة. يرجى التحقق من اتصالك."
    override val serverError = "خطأ في الخادم. يرجى المحاولة لاحقاً."
    override val unknownError = "حدث خطأ غير متوقع."
    override val sessionExpired = "انتهت الجلسة. يرجى البحث مرة أخرى."

    // Velocity UI - Sentence Builder
    override val velocitySentencePrefix = "أبي أسافر"
    override val velocitySentenceFrom = "من"
    override val velocitySentenceTo = "إلى"
    override val velocitySentenceDeparting = "بتاريخ"
    override val velocitySentenceReturning = "والعودة بتاريخ"
    override val velocitySentenceWith = "لعدد"
    override val velocitySelectOrigin = "المغادرة"
    override val velocitySelectDestination = "الوجهة"
    override val velocitySelectDate = "التاريخ"
    override val velocitySelectReturnDate = "العودة"
    override val velocityAdult = "بالغ"
    override val velocityAdults = "بالغين"
    override val velocityOneWay = "ذهاب فقط"
    override val velocityRoundTrip = "ذهاب وعودة"
    override val velocityMultiCity = "وجهات متعددة"
    override val velocityTripType = "نوع الرحلة"

    // Velocity UI - Results
    override val velocityResultsTitle = "الرحلات المتاحة"
    override val velocityFromPrice = "من"
    override val velocityNoResults = "لا توجد رحلات"
    override val velocityNoResultsHint = "جرب تاريخاً أو مساراً مختلفاً"
    override val velocitySearching = "جاري البحث عن الرحلات..."

    // Landing Page
    override val landingHeroTitle1 = "السفر أصبح سهلاً."
    override val landingHeroTitle2 = "احجز مغامرتك القادمة"
    override val landingHeroSubtitle = "اكتشف وجهات مذهلة في الشرق الأوسط وما وراءه مع خدمة حائزة على جوائز وأفضل الأسعار."
    override val landingSearchFlights = "ابحث عن رحلات"
    override val landingFaresFrom = "الأسعار تبدأ من ١٩٩ ريال"
    override val landingSignIn = "تسجيل الدخول"
    override val landingSpecialOffers = "عروض خاصة"
    override val landingSpecialOffersSubtitle = "عروض محدودة الوقت على المسارات الشائعة"
    override val landingPopularDestinations = "الوجهات الشائعة"
    override val landingPopularDestinationsSubtitle = "إلى أين يتوجه المسافرون"
    override val landingWhyUs = "لماذا فير إير؟"
    override val landingWhyUsSubtitle = "سافر بثقة"
    override val landingBestPrice = "ضمان أفضل سعر"
    override val landingBestPriceDesc = "وجدت سعراً أقل؟ سنطابقه."
    override val landingFlexibleBooking = "حجز مرن"
    override val landingFlexibleBookingDesc = "تغييرات مجانية حتى ٢٤ ساعة قبل المغادرة."
    override val landingSupport = "دعم على مدار الساعة"
    override val landingSupportDesc = "فريقنا متواجد دائماً لمساعدتك."
    override val landingRewards = "اكسب مكافآت"
    override val landingRewardsDesc = "نقاط على كل رحلة تحجزها."
    override val landingFlashSale = "عرض سريع"
    override val landingPercentOff = "خصم ٢٠٪"
    override val landingNewRoute = "مسار جديد"
    override val landingPopular = "شائع"
    override val landingFrom = "من"
    override val landingFooterAbout = "عن الشركة"
    override val landingFooterHelp = "المساعدة"
    override val landingFooterPrivacy = "الخصوصية"
    override val landingFooterTerms = "الشروط"
    override val landingFooterCopyright = "© ٢٠٢٤ فير إير. جميع الحقوق محفوظة."
    override val landingFooterPortfolio = "مشروع محفظة أعمال"
    override val landingSignOut = "تسجيل الخروج"
    override val landingMyBookings = "حجوزاتي"
    override val landingWelcome = "مرحباً"
}

/**
 * Gets the strings for the specified language code.
 */
fun getStrings(languageCode: String): AppStrings {
    return when (languageCode) {
        "ar" -> ArabicStrings
        else -> EnglishStrings
    }
}
