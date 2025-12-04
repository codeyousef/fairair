package com.fairair.service

import com.fairair.contract.model.Money
import com.fairair.controller.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for content operations.
 * Handles help center, destinations, newsletter, and contact forms.
 *
 * Production Notes:
 * - Store content in CMS (Strapi, Contentful, etc.)
 * - Integrate with email service for newsletter (Mailchimp, SendGrid)
 * - Use ticketing system for contact forms (Zendesk, Freshdesk)
 */
@Service
class ContentService {
    private val log = LoggerFactory.getLogger(ContentService::class.java)

    // In-memory storage (mock)
    private val newsletterSubscribers = ConcurrentHashMap<String, NewsletterSubscriber>()
    private val contactTickets = ConcurrentHashMap<String, ContactTicket>()

    companion object {
        // Help center categories
        private val HELP_CATEGORIES = listOf(
            HelpCategory(
                id = "booking",
                name = "Booking & Reservations",
                nameAr = "الحجز والحجوزات",
                description = "How to book flights, manage reservations, and payment options",
                descriptionAr = "كيفية حجز الرحلات وإدارة الحجوزات وخيارات الدفع",
                icon = "✈️",
                articleCount = 12
            ),
            HelpCategory(
                id = "checkin",
                name = "Check-in & Boarding",
                nameAr = "تسجيل الوصول والصعود",
                description = "Online check-in, boarding passes, and airport procedures",
                descriptionAr = "تسجيل الوصول عبر الإنترنت، بطاقات الصعود، وإجراءات المطار",
                icon = "🎫",
                articleCount = 8
            ),
            HelpCategory(
                id = "baggage",
                name = "Baggage",
                nameAr = "الأمتعة",
                description = "Baggage allowance, fees, and special items",
                descriptionAr = "حدود الأمتعة، الرسوم، والعناصر الخاصة",
                icon = "🧳",
                articleCount = 10
            ),
            HelpCategory(
                id = "refunds",
                name = "Refunds & Cancellations",
                nameAr = "المبالغ المستردة والإلغاءات",
                description = "How to cancel or change your booking, refund policies",
                descriptionAr = "كيفية إلغاء أو تغيير حجزك، سياسات الاسترداد",
                icon = "💰",
                articleCount = 6
            ),
            HelpCategory(
                id = "membership",
                name = "FairAir Membership",
                nameAr = "عضوية فير إير",
                description = "Subscription plans, benefits, and account management",
                descriptionAr = "خطط الاشتراك، المزايا، وإدارة الحساب",
                icon = "⭐",
                articleCount = 7
            ),
            HelpCategory(
                id = "special",
                name = "Special Assistance",
                nameAr = "المساعدة الخاصة",
                description = "Travelling with disabilities, medical conditions, or children",
                descriptionAr = "السفر مع الإعاقة، الحالات الطبية، أو الأطفال",
                icon = "♿",
                articleCount = 9
            )
        )

        // Help articles
        private val HELP_ARTICLES = listOf(
            // Booking category
            HelpArticle(
                id = "how-to-book",
                categoryId = "booking",
                title = "How to Book a Flight",
                titleAr = "كيفية حجز رحلة",
                summary = "Complete guide to booking your flight",
                summaryAr = "دليل كامل لحجز رحلتك",
                content = """
                    <h3>How to Book a Flight with FairAir</h3>
                    <p>Booking a flight with FairAir is quick and easy. Follow these steps:</p>
                    <ol>
                        <li>Visit our website or open the app</li>
                        <li>Select your departure and arrival cities</li>
                        <li>Choose your travel dates</li>
                        <li>Select the number of passengers</li>
                        <li>Browse available flights and select your preferred option</li>
                        <li>Enter passenger details</li>
                        <li>Add any extras (seats, meals, baggage)</li>
                        <li>Complete payment</li>
                    </ol>
                    <p>You'll receive your confirmation email within minutes.</p>
                """.trimIndent(),
                contentAr = """
                    <h3>كيفية حجز رحلة مع فير إير</h3>
                    <p>حجز رحلة مع فير إير سريع وسهل. اتبع هذه الخطوات:</p>
                    <ol>
                        <li>قم بزيارة موقعنا أو افتح التطبيق</li>
                        <li>اختر مدن المغادرة والوصول</li>
                        <li>اختر تواريخ سفرك</li>
                        <li>حدد عدد المسافرين</li>
                        <li>تصفح الرحلات المتاحة واختر المناسب</li>
                        <li>أدخل تفاصيل المسافرين</li>
                        <li>أضف أي إضافات</li>
                        <li>أكمل الدفع</li>
                    </ol>
                """.trimIndent(),
                tags = listOf("booking", "how-to", "guide", "flights")
            ),
            HelpArticle(
                id = "booking-001",
                categoryId = "booking",
                title = "How do I book a flight?",
                titleAr = "كيف أحجز رحلة؟",
                summary = "Step-by-step guide to booking your flight online",
                summaryAr = "دليل خطوة بخطوة لحجز رحلتك عبر الإنترنت",
                content = """
                    <h3>Booking a Flight with FairAir</h3>
                    <p>Follow these simple steps to book your flight:</p>
                    <ol>
                        <li><strong>Select your trip:</strong> Choose between one-way or round-trip, enter your origin and destination cities, select your travel dates, and specify the number of passengers.</li>
                        <li><strong>Choose your flight:</strong> Browse available flights and compare times and prices. Select your preferred fare type (Fly, Fly+, or FlyMax).</li>
                        <li><strong>Enter passenger details:</strong> Provide the required information for all travelers including names (as per ID), date of birth, and contact information.</li>
                        <li><strong>Add extras:</strong> Select seats, add baggage, pre-order meals, or purchase other ancillary services.</li>
                        <li><strong>Pay securely:</strong> Complete your booking using credit/debit card or other available payment methods.</li>
                        <li><strong>Receive confirmation:</strong> Your booking confirmation and e-ticket will be sent to your email.</li>
                    </ol>
                    <p>Need help? Contact our 24/7 customer support.</p>
                """.trimIndent(),
                contentAr = """
                    <h3>حجز رحلة مع فير إير</h3>
                    <p>اتبع هذه الخطوات البسيطة لحجز رحلتك:</p>
                    <ol>
                        <li><strong>اختر رحلتك:</strong> اختر بين ذهاب فقط أو ذهاب وعودة، أدخل مدن المغادرة والوصول، اختر تواريخ سفرك، وحدد عدد المسافرين.</li>
                        <li><strong>اختر رحلتك:</strong> تصفح الرحلات المتاحة وقارن الأوقات والأسعار. اختر نوع التعرفة المفضل لديك.</li>
                        <li><strong>أدخل تفاصيل المسافرين:</strong> قدم المعلومات المطلوبة لجميع المسافرين.</li>
                        <li><strong>أضف الإضافات:</strong> اختر المقاعد، أضف الأمتعة، اطلب الوجبات مسبقاً.</li>
                        <li><strong>ادفع بأمان:</strong> أكمل حجزك باستخدام بطاقة الائتمان/الخصم.</li>
                        <li><strong>استلم التأكيد:</strong> سيتم إرسال تأكيد حجزك وتذكرتك الإلكترونية إلى بريدك الإلكتروني.</li>
                    </ol>
                """.trimIndent(),
                tags = listOf("booking", "how-to", "flights", "reservation")
            ),
            HelpArticle(
                id = "booking-002",
                categoryId = "booking",
                title = "What payment methods are accepted?",
                titleAr = "ما هي طرق الدفع المقبولة؟",
                summary = "Information about accepted payment methods",
                summaryAr = "معلومات حول طرق الدفع المقبولة",
                content = """
                    <h3>Accepted Payment Methods</h3>
                    <p>FairAir accepts the following payment methods:</p>
                    <ul>
                        <li><strong>Credit Cards:</strong> Visa, Mastercard, American Express</li>
                        <li><strong>Debit Cards:</strong> Mada (Saudi debit cards), Visa Debit, Mastercard Debit</li>
                        <li><strong>Digital Wallets:</strong> Apple Pay, STC Pay</li>
                        <li><strong>Membership Credits:</strong> If you have an active FairAir membership subscription</li>
                    </ul>
                    <p>All transactions are processed securely with 3D Secure authentication.</p>
                """.trimIndent(),
                contentAr = """
                    <h3>طرق الدفع المقبولة</h3>
                    <p>تقبل فير إير طرق الدفع التالية:</p>
                    <ul>
                        <li><strong>بطاقات الائتمان:</strong> فيزا، ماستركارد، أمريكان إكسبريس</li>
                        <li><strong>بطاقات الخصم:</strong> مدى، فيزا ديبت، ماستركارد ديبت</li>
                        <li><strong>المحافظ الرقمية:</strong> أبل باي، STC Pay</li>
                        <li><strong>رصيد العضوية:</strong> إذا كان لديك اشتراك عضوية نشط</li>
                    </ul>
                """.trimIndent(),
                tags = listOf("payment", "credit-card", "mada", "apple-pay")
            ),
            HelpArticle(
                id = "booking-003",
                categoryId = "booking",
                title = "Can I book for someone else?",
                titleAr = "هل يمكنني الحجز لشخص آخر؟",
                summary = "How to book flights for other passengers",
                summaryAr = "كيفية حجز رحلات لمسافرين آخرين",
                content = """
                    <h3>Booking for Others</h3>
                    <p>Yes, you can book flights for other passengers. Here's what you need to know:</p>
                    <ul>
                        <li>Enter the passenger's name exactly as it appears on their ID/passport</li>
                        <li>Provide accurate date of birth and nationality</li>
                        <li>Use a valid email and phone number for booking confirmation</li>
                        <li>The booking confirmation will be sent to the email you provide</li>
                    </ul>
                    <p><strong>Important:</strong> Passenger names cannot be changed after booking. Please double-check all details before confirming your booking.</p>
                """.trimIndent(),
                contentAr = """
                    <h3>الحجز للآخرين</h3>
                    <p>نعم، يمكنك حجز رحلات لمسافرين آخرين. إليك ما تحتاج لمعرفته:</p>
                    <ul>
                        <li>أدخل اسم المسافر كما يظهر بالضبط على هويته/جواز سفره</li>
                        <li>قدم تاريخ الميلاد والجنسية بدقة</li>
                        <li>استخدم بريداً إلكترونياً ورقم هاتف صالحين</li>
                    </ul>
                    <p><strong>مهم:</strong> لا يمكن تغيير أسماء المسافرين بعد الحجز.</p>
                """.trimIndent(),
                tags = listOf("booking", "passengers", "group-booking")
            ),
            // Check-in category
            HelpArticle(
                id = "checkin-001",
                categoryId = "checkin",
                title = "How do I check in online?",
                titleAr = "كيف أسجل الوصول عبر الإنترنت؟",
                summary = "Step-by-step guide for online check-in",
                summaryAr = "دليل خطوة بخطوة لتسجيل الوصول عبر الإنترنت",
                content = """
                    <h3>Online Check-in</h3>
                    <p>Online check-in opens 48 hours before departure and closes 4 hours before your flight.</p>
                    <h4>Steps to Check In:</h4>
                    <ol>
                        <li>Go to "Check-in" on our website or app</li>
                        <li>Enter your booking reference (PNR) and last name</li>
                        <li>Select the passengers you want to check in</li>
                        <li>Choose your seat preferences (if not already selected)</li>
                        <li>Download or email your boarding pass</li>
                    </ol>
                    <p><strong>Tip:</strong> Save your boarding pass to your phone's wallet for easy access at the airport.</p>
                """.trimIndent(),
                contentAr = """
                    <h3>تسجيل الوصول عبر الإنترنت</h3>
                    <p>يفتح تسجيل الوصول عبر الإنترنت قبل 48 ساعة من المغادرة ويغلق قبل 4 ساعات من رحلتك.</p>
                    <h4>خطوات تسجيل الوصول:</h4>
                    <ol>
                        <li>انتقل إلى "تسجيل الوصول" على موقعنا أو تطبيقنا</li>
                        <li>أدخل مرجع الحجز (PNR) واسم العائلة</li>
                        <li>اختر المسافرين الذين تريد تسجيل وصولهم</li>
                        <li>اختر تفضيلات مقعدك</li>
                        <li>حمّل أو أرسل بطاقة الصعود بالبريد الإلكتروني</li>
                    </ol>
                """.trimIndent(),
                tags = listOf("check-in", "online", "boarding-pass")
            ),
            HelpArticle(
                id = "checkin-002",
                categoryId = "checkin",
                title = "What documents do I need at the airport?",
                titleAr = "ما هي المستندات التي أحتاجها في المطار؟",
                summary = "Required travel documents for your journey",
                summaryAr = "مستندات السفر المطلوبة لرحلتك",
                content = """
                    <h3>Required Documents</h3>
                    <h4>For Domestic Flights (within Saudi Arabia):</h4>
                    <ul>
                        <li>Saudi National ID (for Saudi citizens)</li>
                        <li>Valid Iqama (for residents)</li>
                        <li>Passport (for visitors)</li>
                    </ul>
                    <h4>For International Flights:</h4>
                    <ul>
                        <li>Valid passport (minimum 6 months validity)</li>
                        <li>Visa for destination country (if required)</li>
                        <li>Any required health documents</li>
                    </ul>
                    <p><strong>Note:</strong> Document requirements may vary by destination. Please check with the embassy of your destination country.</p>
                """.trimIndent(),
                contentAr = """
                    <h3>المستندات المطلوبة</h3>
                    <h4>للرحلات الداخلية (داخل المملكة العربية السعودية):</h4>
                    <ul>
                        <li>الهوية الوطنية السعودية (للمواطنين السعوديين)</li>
                        <li>إقامة سارية (للمقيمين)</li>
                        <li>جواز السفر (للزوار)</li>
                    </ul>
                    <h4>للرحلات الدولية:</h4>
                    <ul>
                        <li>جواز سفر ساري (صلاحية 6 أشهر على الأقل)</li>
                        <li>تأشيرة لبلد الوجهة (إذا لزم الأمر)</li>
                    </ul>
                """.trimIndent(),
                tags = listOf("documents", "passport", "id", "visa")
            ),
            // Baggage category
            HelpArticle(
                id = "baggage-001",
                categoryId = "baggage",
                title = "What is my baggage allowance?",
                titleAr = "ما هو حد الأمتعة المسموح به؟",
                summary = "Baggage allowance by fare type",
                summaryAr = "حد الأمتعة حسب نوع التعرفة",
                content = """
                    <h3>Baggage Allowance</h3>
                    <table>
                        <tr><th>Fare Type</th><th>Cabin Bag</th><th>Checked Bag</th></tr>
                        <tr><td>Fly (Basic)</td><td>7kg under-seat bag</td><td>Not included (purchase separately)</td></tr>
                        <tr><td>Fly+ (Value)</td><td>7kg + 10kg cabin bag</td><td>20kg included</td></tr>
                        <tr><td>FlyMax (Flex)</td><td>7kg + 10kg cabin bag</td><td>30kg included</td></tr>
                    </table>
                    <h4>Additional Baggage:</h4>
                    <p>You can purchase additional checked baggage:</p>
                    <ul>
                        <li>15kg bag: SAR 75</li>
                        <li>20kg bag: SAR 95</li>
                        <li>30kg bag: SAR 145</li>
                    </ul>
                    <p><strong>Tip:</strong> Purchase baggage online during booking for the best rates. Airport prices are higher.</p>
                """.trimIndent(),
                contentAr = """
                    <h3>حد الأمتعة</h3>
                    <h4>الأمتعة الإضافية:</h4>
                    <p>يمكنك شراء أمتعة مسجلة إضافية:</p>
                    <ul>
                        <li>حقيبة 15 كجم: 75 ريال</li>
                        <li>حقيبة 20 كجم: 95 ريال</li>
                        <li>حقيبة 30 كجم: 145 ريال</li>
                    </ul>
                """.trimIndent(),
                tags = listOf("baggage", "allowance", "checked-bag", "cabin-bag")
            ),
            // Refunds category
            HelpArticle(
                id = "refunds-001",
                categoryId = "refunds",
                title = "How do I cancel my booking?",
                titleAr = "كيف ألغي حجزي؟",
                summary = "Guide to cancelling your flight booking",
                summaryAr = "دليل لإلغاء حجز رحلتك",
                content = """
                    <h3>Cancelling Your Booking</h3>
                    <p>Cancellation policies depend on your fare type:</p>
                    <ul>
                        <li><strong>Fly (Basic):</strong> Non-refundable. Airport taxes may be refunded.</li>
                        <li><strong>Fly+ (Value):</strong> SAR 150 cancellation fee. Refund as credit.</li>
                        <li><strong>FlyMax (Flex):</strong> Free cancellation up to 24 hours before departure.</li>
                    </ul>
                    <h4>To Cancel:</h4>
                    <ol>
                        <li>Go to "Manage Booking"</li>
                        <li>Enter your PNR and last name</li>
                        <li>Select "Cancel Booking"</li>
                        <li>Confirm your cancellation</li>
                    </ol>
                    <p>Refunds are processed within 7-14 business days.</p>
                """.trimIndent(),
                contentAr = """
                    <h3>إلغاء حجزك</h3>
                    <p>تعتمد سياسات الإلغاء على نوع تعرفتك:</p>
                    <ul>
                        <li><strong>Fly (الأساسي):</strong> غير قابل للاسترداد. قد يتم استرداد ضرائب المطار.</li>
                        <li><strong>Fly+ (القيمة):</strong> رسوم إلغاء 150 ريال. الاسترداد كرصيد.</li>
                        <li><strong>FlyMax (المرن):</strong> إلغاء مجاني حتى 24 ساعة قبل المغادرة.</li>
                    </ul>
                """.trimIndent(),
                tags = listOf("cancel", "refund", "booking", "policy")
            ),
            // Membership category
            HelpArticle(
                id = "membership-001",
                categoryId = "membership",
                title = "What is FairAir Membership?",
                titleAr = "ما هي عضوية فير إير؟",
                summary = "Learn about our subscription plans",
                summaryAr = "تعرف على خطط الاشتراك لدينا",
                content = """
                    <h3>FairAir Membership</h3>
                    <p>FairAir Membership is a subscription service that lets you fly more for less.</p>
                    <h4>Plans Available:</h4>
                    <ul>
                        <li><strong>Basic (12 trips/year):</strong> SAR 299/month - 1 round trip per month</li>
                        <li><strong>Standard (24 trips/year):</strong> SAR 549/month - 2 round trips per month</li>
                        <li><strong>Premium (36 trips/year):</strong> SAR 799/month - 3 round trips per month</li>
                    </ul>
                    <h4>Benefits:</h4>
                    <ul>
                        <li>No booking fees</li>
                        <li>Priority check-in</li>
                        <li>Included baggage (varies by plan)</li>
                        <li>Flexible booking up to 3 days before departure</li>
                    </ul>
                """.trimIndent(),
                contentAr = """
                    <h3>عضوية فير إير</h3>
                    <p>عضوية فير إير هي خدمة اشتراك تتيح لك السفر أكثر بتكلفة أقل.</p>
                    <h4>الخطط المتاحة:</h4>
                    <ul>
                        <li><strong>الأساسي (12 رحلة/سنة):</strong> 299 ريال/شهر</li>
                        <li><strong>القياسي (24 رحلة/سنة):</strong> 549 ريال/شهر</li>
                        <li><strong>المميز (36 رحلة/سنة):</strong> 799 ريال/شهر</li>
                    </ul>
                """.trimIndent(),
                tags = listOf("membership", "subscription", "plans", "benefits")
            ),
            // Special assistance category
            HelpArticle(
                id = "special-001",
                categoryId = "special",
                title = "Travelling with infants and children",
                titleAr = "السفر مع الرضع والأطفال",
                summary = "Information for families travelling with young children",
                summaryAr = "معلومات للعائلات المسافرة مع أطفال صغار",
                content = """
                    <h3>Travelling with Children</h3>
                    <h4>Infants (0-23 months):</h4>
                    <ul>
                        <li>Infants travel on an adult's lap at a discounted fare</li>
                        <li>One infant per adult passenger</li>
                        <li>Bassinet available on request (limited availability)</li>
                        <li>You may bring a stroller and car seat free of charge</li>
                    </ul>
                    <h4>Children (2-11 years):</h4>
                    <ul>
                        <li>Children require their own seat</li>
                        <li>Child fares may apply</li>
                        <li>Unaccompanied minors service available for ages 5-12</li>
                    </ul>
                    <h4>Tips for Travelling with Kids:</h4>
                    <ul>
                        <li>Book early to get seats together</li>
                        <li>Pre-order kids meals for better options</li>
                        <li>Bring entertainment and snacks</li>
                    </ul>
                """.trimIndent(),
                contentAr = """
                    <h3>السفر مع الأطفال</h3>
                    <h4>الرضع (0-23 شهراً):</h4>
                    <ul>
                        <li>يسافر الرضع على حضن شخص بالغ بسعر مخفض</li>
                        <li>رضيع واحد لكل راكب بالغ</li>
                        <li>يمكنك إحضار عربة أطفال ومقعد سيارة مجاناً</li>
                    </ul>
                    <h4>الأطفال (2-11 سنة):</h4>
                    <ul>
                        <li>يحتاج الأطفال إلى مقعد خاص بهم</li>
                        <li>قد تنطبق أسعار الأطفال</li>
                    </ul>
                """.trimIndent(),
                tags = listOf("children", "infants", "family", "unaccompanied-minor")
            )
        )

        // Destinations
        private val DESTINATIONS = listOf(
            DestinationInfo(
                code = "RUH",
                name = "Riyadh",
                nameAr = "الرياض",
                country = "Saudi Arabia",
                countryAr = "المملكة العربية السعودية",
                description = "The capital city of Saudi Arabia, a modern metropolis blending ancient traditions with contemporary innovation.",
                descriptionAr = "العاصمة السعودية، مدينة عصرية تمزج بين التقاليد العريقة والابتكار المعاصر.",
                imageUrl = "/images/destinations/riyadh.jpg",
                galleryImages = listOf("/images/destinations/riyadh-1.jpg", "/images/destinations/riyadh-2.jpg"),
                highlights = listOf(
                    DestinationHighlight("Kingdom Tower", "برج المملكة", "Iconic landmark with sky bridge", "معلم بارز مع جسر السماء", "🏙️"),
                    DestinationHighlight("Diriyah", "الدرعية", "UNESCO World Heritage site", "موقع تراث عالمي لليونسكو", "🏛️"),
                    DestinationHighlight("Boulevard Riyadh", "بوليفارد الرياض", "Entertainment district", "منطقة ترفيهية", "🎭")
                ),
                weather = WeatherInfo(28, "Hot and dry summers, mild winters", "صيف حار وجاف، شتاء معتدل"),
                timezone = "Asia/Riyadh (GMT+3)",
                currency = "SAR",
                language = "Arabic",
                lowestFare = Money.sar(199.0),
                popularRoutes = listOf(
                    PopularRoute("JED", "Jeddah", Money.sar(199.0), "1h 30m"),
                    PopularRoute("DMM", "Dammam", Money.sar(149.0), "1h 15m")
                )
            ),
            DestinationInfo(
                code = "JED",
                name = "Jeddah",
                nameAr = "جدة",
                country = "Saudi Arabia",
                countryAr = "المملكة العربية السعودية",
                description = "Gateway to the Holy Cities, known for its historic old town and beautiful Red Sea corniche.",
                descriptionAr = "بوابة المدينتين المقدستين، معروفة بمدينتها القديمة التاريخية وكورنيش البحر الأحمر الجميل.",
                imageUrl = "/images/destinations/jeddah.jpg",
                galleryImages = listOf("/images/destinations/jeddah-1.jpg"),
                highlights = listOf(
                    DestinationHighlight("Al-Balad", "البلد", "Historic old town, UNESCO site", "المدينة القديمة التاريخية", "🏛️"),
                    DestinationHighlight("King Fahd Fountain", "نافورة الملك فهد", "World's tallest fountain", "أطول نافورة في العالم", "⛲"),
                    DestinationHighlight("Red Sea Corniche", "كورنيش البحر الأحمر", "Beautiful waterfront promenade", "ممشى واجهة بحرية جميل", "🌊")
                ),
                weather = WeatherInfo(32, "Humid coastal climate", "مناخ ساحلي رطب"),
                timezone = "Asia/Riyadh (GMT+3)",
                currency = "SAR",
                language = "Arabic",
                lowestFare = Money.sar(199.0),
                popularRoutes = listOf(
                    PopularRoute("RUH", "Riyadh", Money.sar(199.0), "1h 30m"),
                    PopularRoute("DMM", "Dammam", Money.sar(249.0), "2h")
                )
            ),
            DestinationInfo(
                code = "DXB",
                name = "Dubai",
                nameAr = "دبي",
                country = "United Arab Emirates",
                countryAr = "الإمارات العربية المتحدة",
                description = "A global hub for tourism and business, famous for luxury shopping and ultramodern architecture.",
                descriptionAr = "مركز عالمي للسياحة والأعمال، مشهورة بالتسوق الفاخر والعمارة فائقة الحداثة.",
                imageUrl = "/images/destinations/dubai.jpg",
                galleryImages = listOf("/images/destinations/dubai-1.jpg"),
                highlights = listOf(
                    DestinationHighlight("Burj Khalifa", "برج خليفة", "World's tallest building", "أطول مبنى في العالم", "🏙️"),
                    DestinationHighlight("Dubai Mall", "دبي مول", "World's largest mall", "أكبر مول في العالم", "🛍️"),
                    DestinationHighlight("Palm Jumeirah", "نخلة جميرا", "Iconic man-made island", "جزيرة اصطناعية مميزة", "🏝️")
                ),
                weather = WeatherInfo(35, "Hot desert climate", "مناخ صحراوي حار"),
                timezone = "Asia/Dubai (GMT+4)",
                currency = "AED",
                language = "Arabic, English",
                lowestFare = Money.sar(399.0),
                popularRoutes = listOf(
                    PopularRoute("JED", "Jeddah", Money.sar(399.0), "2h 30m"),
                    PopularRoute("RUH", "Riyadh", Money.sar(349.0), "2h")
                )
            )
        )
    }

    // ========================================================================
    // Help Center
    // ========================================================================

    suspend fun getHelpCategories(): List<HelpCategory> {
        log.info("Getting help categories")
        return HELP_CATEGORIES
    }

    suspend fun getArticlesInCategory(categoryId: String): CategoryArticlesResponse {
        log.info("Getting articles for category=$categoryId")

        val category = HELP_CATEGORIES.find { it.id == categoryId }
            ?: throw CategoryNotFoundException(categoryId)

        val articles = HELP_ARTICLES.filter { it.categoryId == categoryId }

        return CategoryArticlesResponse(
            categoryId = categoryId,
            categoryName = category.name,
            articles = articles
        )
    }

    suspend fun getArticle(articleId: String): HelpArticle {
        log.info("Getting article=$articleId")

        return HELP_ARTICLES.find { it.id == articleId }
            ?: throw ArticleNotFoundException(articleId)
    }

    suspend fun searchArticles(query: String, lang: String): List<HelpArticle> {
        log.info("Searching articles: query=$query, lang=$lang")

        val searchTerms = query.lowercase().split(" ")

        return HELP_ARTICLES.filter { article ->
            val searchText = if (lang == "ar") {
                "${article.titleAr} ${article.contentAr} ${article.tags.joinToString(" ")}"
            } else {
                "${article.title} ${article.content} ${article.tags.joinToString(" ")}"
            }.lowercase()

            searchTerms.any { term -> searchText.contains(term) }
        }
    }

    // ========================================================================
    // Destinations
    // ========================================================================

    suspend fun getDestinations(): List<DestinationSummary> {
        log.info("Getting all destinations")

        return DESTINATIONS.map { dest ->
            DestinationSummary(
                code = dest.code,
                name = dest.name,
                nameAr = dest.nameAr,
                country = dest.country,
                countryAr = dest.countryAr,
                description = dest.description,
                descriptionAr = dest.descriptionAr,
                imageUrl = dest.imageUrl,
                lowestFare = dest.lowestFare
            )
        }
    }

    suspend fun getDestinationDetails(code: String): DestinationDetail {
        log.info("Getting destination details: $code")

        val dest = DESTINATIONS.find { it.code == code }
            ?: throw DestinationNotFoundException(code)

        return DestinationDetail(
            code = dest.code,
            name = dest.name,
            nameAr = dest.nameAr,
            country = dest.country,
            countryAr = dest.countryAr,
            description = dest.description,
            descriptionAr = dest.descriptionAr,
            imageUrl = dest.imageUrl,
            galleryImages = dest.galleryImages,
            highlights = dest.highlights,
            weather = dest.weather,
            timezone = dest.timezone,
            currency = dest.currency,
            language = dest.language,
            lowestFare = dest.lowestFare,
            popularRoutes = dest.popularRoutes
        )
    }

    // ========================================================================
    // Newsletter
    // ========================================================================

    suspend fun subscribeToNewsletter(
        email: String,
        name: String?,
        preferences: List<String>?
    ): NewsletterSubscribeResponse {
        log.info("Subscribing to newsletter: $email")

        // Validate email
        if (!email.matches(Regex("^[A-Za-z0-9+_.-]+@(.+)$"))) {
            throw IllegalArgumentException("Invalid email format")
        }

        // Check for existing subscription
        if (newsletterSubscribers.containsKey(email.lowercase())) {
            throw NewsletterAlreadySubscribedException(email)
        }

        val subscriber = NewsletterSubscriber(
            id = UUID.randomUUID().toString(),
            email = email.lowercase(),
            name = name,
            preferences = preferences ?: listOf("deals", "news"),
            subscribedAt = System.currentTimeMillis()
        )

        newsletterSubscribers[email.lowercase()] = subscriber

        return NewsletterSubscribeResponse(
            success = true,
            email = email.lowercase(),
            message = "Successfully subscribed to our newsletter!",
            subscriptionId = subscriber.id
        )
    }

    suspend fun unsubscribeFromNewsletter(email: String, token: String) {
        log.info("Unsubscribing from newsletter: $email")

        val subscriber = newsletterSubscribers[email.lowercase()]
        // Accept either subscriptionId or email as token for flexibility
        if (subscriber == null || (subscriber.id != token && email.lowercase() != token.lowercase())) {
            throw InvalidTokenException("Invalid unsubscribe token")
        }

        newsletterSubscribers.remove(email.lowercase())
    }

    // ========================================================================
    // Contact Form
    // ========================================================================

    suspend fun submitContactForm(
        name: String,
        email: String,
        phone: String?,
        subject: String,
        category: String,
        message: String,
        bookingReference: String?
    ): ContactFormResponse {
        log.info("Contact form submission: $subject from $email")

        // Validate required fields
        if (name.isBlank()) throw IllegalArgumentException("Name is required")
        if (email.isBlank()) throw IllegalArgumentException("Email is required")
        if (subject.isBlank()) throw IllegalArgumentException("Subject is required")
        if (message.isBlank()) throw IllegalArgumentException("Message is required")

        // Validate email format
        if (!email.matches(Regex("^[A-Za-z0-9+_.-]+@(.+)$"))) {
            throw IllegalArgumentException("Invalid email format")
        }

        val ticketId = "TKT-${System.currentTimeMillis().toString().takeLast(8)}"

        val ticket = ContactTicket(
            ticketId = ticketId,
            name = name,
            email = email,
            phone = phone,
            subject = subject,
            category = category,
            message = message,
            bookingReference = bookingReference,
            createdAt = System.currentTimeMillis(),
            status = "OPEN"
        )

        contactTickets[ticketId] = ticket

        // In production: Send confirmation email, create ticket in support system

        return ContactFormResponse(
            success = true,
            ticketNumber = ticketId,
            message = "Thank you for contacting us. We have received your message and will respond within 24-48 hours.",
            estimatedResponseTime = "24-48 hours"
        )
    }

    // ========================================================================
    // FAQ
    // ========================================================================

    suspend fun getFAQs(category: String?): List<FAQ> {
        log.info("Getting FAQs, category=$category")

        val faqs = listOf(
            FAQ("1", "How do I book a flight?", "You can book a flight through our website or mobile app by selecting your destination, dates, and passengers.", "booking"),
            FAQ("2", "What is the baggage allowance?", "The baggage allowance depends on your fare type. Light fare: 7kg cabin bag only. Value fare: 7kg cabin bag + 20kg checked bag.", "baggage"),
            FAQ("3", "Can I change my flight?", "Yes, you can change your flight subject to availability and fare difference. Changes can be made online up to 4 hours before departure.", "booking"),
            FAQ("4", "How do I check in online?", "Online check-in opens 48 hours before departure. Visit our website or app and enter your booking reference.", "checkin"),
            FAQ("5", "What documents do I need?", "You need a valid passport or ID card (for domestic flights). Check visa requirements for your destination.", "travel"),
            FAQ("6", "How do I cancel my booking?", "You can cancel your booking online. Refunds are subject to fare conditions and cancellation fees.", "booking"),
            FAQ("7", "Do you offer special meals?", "Yes, we offer various special meals including vegetarian, halal, and children's meals. Pre-order at least 24 hours before departure.", "services"),
            FAQ("8", "How do I select my seat?", "Seats can be selected during booking or anytime before check-in through our website or app.", "services"),
            FAQ("9", "What if my flight is delayed?", "We will notify you via SMS/email. You may be entitled to compensation depending on the delay length.", "travel"),
            FAQ("10", "How do I contact customer service?", "You can reach us via phone, email, or chat. Contact details are available on our website.", "general")
        )

        return if (category != null) {
            faqs.filter { it.category == category }
        } else {
            faqs
        }
    }
}

data class FAQ(
    val id: String,
    val question: String,
    val answer: String,
    val category: String
)

// Internal data classes
private data class DestinationInfo(
    val code: String,
    val name: String,
    val nameAr: String,
    val country: String,
    val countryAr: String,
    val description: String,
    val descriptionAr: String,
    val imageUrl: String,
    val galleryImages: List<String>,
    val highlights: List<DestinationHighlight>,
    val weather: WeatherInfo,
    val timezone: String,
    val currency: String,
    val language: String,
    val lowestFare: Money,
    val popularRoutes: List<PopularRoute>
)

private data class NewsletterSubscriber(
    val id: String,
    val email: String,
    val name: String?,
    val preferences: List<String>,
    val subscribedAt: Long
)

private data class ContactTicket(
    val ticketId: String,
    val name: String,
    val email: String,
    val phone: String?,
    val subject: String,
    val category: String,
    val message: String,
    val bookingReference: String?,
    val createdAt: Long,
    val status: String
)
