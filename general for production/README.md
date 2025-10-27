# 🌌 Spectre Guild Web Platform

Welcome to the official **Spectre Guild** platform – a full-stack web application built for the Star Citizen guild "Spectre".  
This platform combines a secure backend, an interactive frontend, and live connections to ship and economy APIs.

---

## ⚙️ Project Overview

**Purpose:**  
To give Spectre guild members access to:
- 🛠️ Interactive tools (ship comparison, trade route planner, commodities lookup)
- 🖼️ Image gallery for in-game screenshots
- 📝 Forum for communication
- 📅 Discord event synchronization
- 🔐 Secure authentication with Discord OAuth2 and JWT

---

## 🗂️ Project Structure

```
Website/
├── backend/
│   ├── src/main/java/com/spectre/
│   │   ├── controller/           # REST API controllers
│   │   │   └── tools/           # Tool-specific controllers
│   │   ├── model/               # JPA entities
│   │   ├── repository/          # Data access layer
│   │   ├── security/            # Authentication & authorization
│   │   │   ├── jwt/            # JWT implementation
│   │   │   ├── services/       # Security services
│   │   │   └── tools/          # Tool-specific services
│   │   ├── payload/            # DTOs and request/response objects
│   │   └── cache/              # Caching layer
│   ├── src/main/resources/
│   │   └── application.properties
│   ├── pom.xml                 # Maven dependencies
│   └── Dockerfile
├── frontend/
│   ├── src/
│   │   ├── components/         # React components
│   │   ├── pages/             # Page components
│   │   │   └── Tools/         # Tool pages
│   │   ├── context/           # React context providers
│   │   ├── api/               # API client functions
│   │   └── hooks/             # Custom React hooks
│   ├── public/                # Static files
│   ├── package.json           # NPM dependencies
│   └── Dockerfile
├── docker-compose.yml         # Docker orchestration
└── website_backup.sql         # Database backup
```

---

## 🛠️ Technologies Used

| Layer      | Stack                                      |
|------------|--------------------------------------------|
| Frontend   | React 19.1.0, Axios, Context API, Recharts, Framer Motion |
| Backend    | Java 21, Spring Boot 3.5.0, Maven         |
| Security   | Discord OAuth2, JWT + Refresh Tokens       |
| Database   | MySQL 8.0 with HikariCP                    |
| APIs       | UEX (commodities), Star Citizen Wiki (ships)|
| Auth       | Role-based via `@PreAuthorize`             |
| Deployment | Docker Compose                             |

---

## 🗃️ MySQL Database Structure

**Current Configuration:**  
- **Docker:** `jdbc:mysql://mysql-db:3306/website`
- **Local:** `jdbc:mysql://localhost:3306/website`

### 📊 Database Tables

#### **👤 User Management**
- `users` — Discord-authenticated users (id, username, discord_id, avatar)
- `roles` — Available roles (`ROLE_GUEST`, `ROLE_USER`, `ROLE_ADMIN`)
- `user_roles` — Many-to-many mapping table
- `refresh_token` — JWT refresh tokens

#### **📝 Forum System**
- `posts` — Forum posts with title, content, timestamps, user_id

#### **🖼️ Image Gallery**
- `images` — BLOB storage for screenshots (LONGBLOB)

#### **🛳️ Ship Database**
- `ships` — Star Citizen ship specifications and data

#### **💰 Commodity Data**
- `price_entries` — Live commodity price data (cached)

### **Role System:**
- `ROLE_GUEST` — Basic access
- `ROLE_USER` — Full tool access
- `ROLE_ADMIN` — Administrative privileges

---

## 🔗 External APIs

- **UEX API**: Live commodity prices and trade data
- **Star Citizen Wiki API**: Ship specifications and images
- **Discord API**: OAuth2 authentication and guild member verification

---

## 🐳 Docker Setup

```yaml
# docker-compose.yml
services:
  mysql:
    image: mysql:8
    container_name: mysql-db
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: rootpas
      MYSQL_DATABASE: website
      MYSQL_USER: "lori"
      MYSQL_PASSWORD: "!Spectre187"
    volumes:
      - website_db_data:/var/lib/mysql
    ports:
      - "3306:3306"

  backend:
    build:
      context: ./backend
    container_name: spectre-api
    depends_on:
      - mysql
    env_file:
      - .env
    ports:
      - "8080:8080"

volumes:
  website_db_data:
```

---

## 🚀 How to Run Locally

### 🧪 Backend (Spring Boot)
```bash
cd backend
mvn clean install
mvn spring-boot:run
```
Runs at: `http://localhost:8080`

### 🌐 Frontend (React)
```bash
cd frontend
npm install
npm start
```
Runs at: `http://localhost:3000`

### 🐳 Docker (Full Stack)
```bash
docker compose up -d
```

---

## 📦 Backend Configuration

### **Key Properties (`application.properties`):**
- **Database:** MySQL with Docker container
- **OAuth2:** Discord integration with guild verification
- **JWT:** 24-hour tokens with refresh capability
- **CORS:** Allowed from `http://localhost:3000`
- **File Upload:** Image storage in database
- **External APIs:** UEX token for commodity data

### **Security Features:**
- Discord OAuth2 authentication
- JWT token-based sessions
- Role-based access control
- Rate limiting
- CORS protection

---

## 🛠️ Available Tools

### **✅ Implemented:**
- [x] **Ship Comparison** — Compare ship specifications side-by-side
- [x] **Ship Information** — Detailed ship data and statistics
- [x] **Commodities Tracker** — Live commodity prices and trade data
- [x] **Trade Route Planner** — Optimize trading routes
- [x] **Image Gallery** — Upload and view in-game screenshots
- [x] **Forum System** — Create and view posts
- [x] **Discord Events** — View guild events

### **🚧 In Development:**
- [ ] **Earnings Tracker** — Calculate and track profit (placeholder)
- [ ] **Admin Dashboard** — Administrative tools (empty)

---

## 🔧 Recent Fixes & Improvements

### **Discord Authentication Issues Fixed:**
- ✅ Added comprehensive error handling for Discord API calls
- ✅ Implemented fallback for non-guild members
- ✅ Added detailed logging for debugging
- ✅ Fixed unused variable warning in AuthContext
- ✅ Created error handling for authentication failures

### **Database Structure:**
- ✅ Proper JPA entity relationships
- ✅ Optimized table structure
- ✅ Caching layer for external API data

---

## 🧭 Roadmap

### **High Priority:**
- [ ] Complete Earnings Tracker implementation
- [ ] Build Admin Dashboard with user management
- [ ] Add Discord event synchronization
- [ ] Implement image optimization (WebP/JPEG compression)

### **Medium Priority:**
- [ ] Add user activity analytics
- [ ] Implement advanced search functionality
- [ ] Add dark mode theme
- [ ] Mobile-responsive improvements

### **Low Priority:**
- [ ] Add ship wishlist feature
- [ ] Implement notifications system
- [ ] Add export functionality for data

---

## 🐛 Known Issues

1. **Earnings Tool:** Currently just a placeholder - needs full implementation
2. **Admin Dashboard:** Empty component - needs development
3. **Local Development:** Requires MySQL setup or Docker
4. **Discord Auth:** Requires proper Discord application configuration

---

## 👤 Author

**Lorenzo Edoardo Giacomelli**  
Built with ❤️ and a fleet of ships in mind.

---

## 📝 Development Notes

- **Database:** Uses MySQL with JPA/Hibernate
- **Authentication:** Discord OAuth2 with JWT tokens
- **Frontend:** React 19 with modern hooks and context
- **Backend:** Spring Boot 3.5 with comprehensive security
- **Deployment:** Docker Compose for containerized deployment
Tailwind CSS	Fast, utility-based, easy to use in Copilot
ShadCN/UI	Beautiful prebuilt UI components (React + Tailwind)
Framer Motion	For beautiful animations (smooth + declarative)
Lucide Icons	Lightweight icon set 