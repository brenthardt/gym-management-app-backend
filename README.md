SEE FRONTEND HERE: https://github.com/brenthardt/gym-management-app-frontend.git

# Gym Management System with Telegram Bot

Bu loyiha gym management tizimi bo'lib, Telegram bot integratsiyasi bilan jihozlangan.

## Asosiy Xususiyatlar

### 🏋️‍♂️ Gym Management

- Gym qo'shish va boshqarish
- Tariff rejalarini boshqarish
- Foydalanuvchilarni boshqarish
- Subscription tizimi

### 🤖 Telegram Bot

- Foydalanuvchilar login qilishi mumkin
- Role-based access (USER, ADMIN, SUPERADMIN)
- Chat ID avtomatik saqlanadi
- Admin panel orqali xabar yuborish
- **Inline qidiruv funksiyasi** - Foydalanuvchilarni telefon raqam bo'yicha qidirish

### 🔐 Security

- JWT authentication
- Role-based authorization
- Spring Security

## Telegram Bot Funksiyalari

### Foydalanuvchi uchun:

- Login qilish
- Shaxsiy ma'lumotlarni ko'rish
- Chat ID ni ko'rish

### Admin uchun:

- Statistika ko'rish
- A'zolarni boshqarish
- **Inline qidiruv** - `@shaxzod333_bot [telefon]` yozib foydalanuvchi qidirish
- **Subscription kamaytirish** - Foydalanuvchi tanlanganda uning subscription durationi 1 kunga kamayadi

### SuperAdmin uchun:

- Yangi gym qo'shish
- Gym ro'yxatini ko'rish
- Foydalanuvchilarni boshqarish
- Barcha foydalanuvchilarga xabar yuborish

## Inline Qidiruv Funksiyasi

### Qanday ishlaydi:

1. Admin `@shaxzod333_bot` yozib, telefon raqamni kiritadi
2. Bot faqat o'sha admin'ning gym'ida bo'lgan va faol obunasi bor foydalanuvchilarni ko'rsatadi
3. Admin foydalanuvchini tanlaydi
4. Bot avtomatik ravishda foydalanuvchining subscription durationini 1 kunga kamaytiradi
5. Bot xabar yuboradi: "✅ [Foydalanuvchi] gym ga keldi va uni kuni 1 kunga kamaydi!"

### Xususiyatlar:

- Faqat telefon raqam bo'yicha qidirish
- Faqat faol obunasi bor foydalanuvchilar ko'rsatiladi
- Admin faqat o'z gym'idagi foydalanuvchilarni ko'radi
- Bir kunda bir marta ishlatish mumkin
- Avtomatik duration kamaytirish

## Chat ID nima va nima uchun kerak?

**Chat ID** - bu Telegram'da har bir foydalanuvchi yoki guruhning noyob identifikatori.

### Nima uchun kerak:

1. **Foydalanuvchini aniqlash** - Bot qaysi foydalanuvchi bilan gaplashayotganini biladi
2. **Xabar yuborish** - Admin panel orqali foydalanuvchilarga xabar yuborish
3. **Notification** - Tizimdan avtomatik xabarlar yuborish
4. **Personalization** - Har bir foydalanuvchi uchun alohida session

### Qanday ishlaydi:

1. Foydalanuvchi telegram botga `/start` yozadi
2. Bot uning Chat ID sini oladi
3. Foydalanuvchi login qilgandan so'ng, Chat ID database'ga saqlanadi
4. Keyinchalik admin panel orqali shu Chat ID ga xabar yuborish mumkin

## API Endpoints

### Telegram Admin Panel

- `POST /api/telegram/send-to-user/{userId}` - Foydalanuvchiga xabar yuborish
- `POST /api/telegram/send-to-chat/{chatId}` - Chat ID ga xabar yuborish
- `POST /api/telegram/send-to-role/{roleName}` - Role bo'yicha xabar yuborish
- `POST /api/telegram/send-to-all` - Barcha foydalanuvchilarga xabar yuborish
- `GET /api/telegram/users-with-telegram` - Telegram bilan bog'langan foydalanuvchilar
- `GET /api/telegram/user/{userId}/has-telegram` - Foydalanuvchida telegram bormi?

## Texnologiyalar

- **Backend**: Spring Boot 3.4.5
- **Database**: PostgreSQL
- **Security**: Spring Security + JWT
- **Telegram**: TelegramBots API 6.8.0
- **Build Tool**: Maven

## Ishga tushirish

1. PostgreSQL database yarating
2. `application.properties` da database va telegram bot sozlamalarini kiriting:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/gym_db
spring.datasource.username=your_username
spring.datasource.password=your_password

# Telegram Bot
telegram.bot.token=YOUR_BOT_TOKEN
telegram.bot.username=YOUR_BOT_USERNAME
telegram.bot.webhook-path=/webhook
```

3. Loyihani ishga tushiring:

```bash
mvn spring-boot:run
```

## Database Schema

User jadvalida yangi maydon qo'shildi:

- `telegram_chat_id` - Foydalanuvchining Telegram Chat ID si

## Clean Code Principles

- SOLID principles
- Clean method names
- Proper error handling
- Logging
- Separation of concerns
- No comments in code (self-documenting code)
