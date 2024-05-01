# AccountWave

A comprehensive Android app for tracking budgets, personal expenses, and inventory with intelligent barcode/receipt scanning and automatic budget alerts.

## ✨ Features

### Core Functionality
- **Dashboard** – Real-time category balances, recent transactions, budget vs. spend charts, and CSV log export
- **Budget Management** – Set per-category spending limits with visual spend tracking and swipe-to-delete
- **Expense/Sales Tracking** – Date-stamped transactions linked to categories and custom tabs
- **Inventory Management** – Track items with quantity, cost, category, and tags; full transaction history
- **Custom Tags** – Organize both expenses and inventory with named tabs
- **Smart Notifications** – Automated alerts when spending reaches 80% or exceeds budget limits
- **Activity Logging** – Complete audit trail of all budgets, inventory, and transactions with CSV export

### Smart Input
- **Barcode Scanning** – Quick item lookup via ML Kit barcode detection
- **Receipt OCR** – Automatic field population from scanned receipts using text recognition

## 🏗️ Architecture

### Data Model (Room Database)
```kotlin
Budget(id, category, limit)
Transaction(id, date, amount, category, title, type, tabName)
InventoryItem(id, name, description, quantity, cost, category, tabId, notes)
Tab(id, name)
LogEntry(id, timestamp, entityName, entityId, operationType, details)
```

### Key Components
- **MainActivity** – Navigation host with bottom navigation (Dashboard, Inventory, Budget)
- **SalesFragment** – Add/manage personal expenses and sales
- **InventoryFragment** – List view with tab/category filters and transaction history
- **AddInventoryFragment** – Create items with scan assistance
- **AddInventoryTransactionFragment** – Stock adjustments with logging
- **BudgetFragment** – Budget CRUD with spend visualization
- **DashboardFragment** – Summary view with charts and export functionality
- **BarcodeScannerFragment** – CameraX + ML Kit integration for scanning

## 🛠️ Tech Stack

- **Language:** Kotlin
- **Database:** Room
- **UI:** Material Components, Jetpack Compose (charts via Vico)
- **Architecture:** Navigation Component, LiveData/ViewModel
- **Background:** WorkManager (periodic budget checks)
- **ML/Camera:** CameraX, ML Kit (barcode + text recognition)

## 📋 Requirements

- Android Studio Giraffe or later
- Android SDK 34+
- Google Maven repository (for ML Kit/CameraX dependencies)

### Permissions
- `CAMERA` – Barcode and receipt scanning
- `POST_NOTIFICATIONS` – Budget alerts (Android 13+)

## 🚀 Getting Started

1. **Clone the repository**
   ```bash
   git clone https://github.com/Nkadankwa/accountwave.git
   cd accountwave
   ```

2. **Open in Android Studio**
   - Let Gradle sync automatically
   - Ensure Google Maven is configured

3. **Build & Run**
   ```bash
   ./gradlew assembleDebug
   ```
   Or use Android Studio's Run button

## 📱 Usage

1. **Setup** – Create budgets and custom tabs for organization
2. **Track Expenses** – Add transactions under categories with optional tab assignment
3. **Manage Inventory** – Add items and adjust stock via inventory transactions
4. **Scan for Speed** – Use camera button in forms to scan barcodes or receipts
5. **Monitor Budgets** – Check Dashboard for real-time balances and visual charts
6. **Export Data** – Download complete activity logs as CSV from Dashboard

## 🔒 Privacy & Data

- No personal data or API keys hardcoded in the application
- All data stored locally on device using Room database
- Activity logs contain only entity operations (budgets, inventory, transactions)
- User controls all data export via CSV functionality

## 📄 License

[Add your license here]

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📧 Contact

[Add your contact information]

---

**Built with ❤️ using Kotlin and Jetpack libraries**