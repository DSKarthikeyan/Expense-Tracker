# Expense-Tracker

The **Expense Module** is an Android application designed to manage and track expenses seamlessly. It leverages modern development practices, including the **MVVM (Model-View-ViewModel)** architecture, ensuring a clean separation of concerns and maintainable code.

## Features
- **Expense Tracking**: Automatically capture expense details from SMS notifications.
- **Add and Manage Expenses**: Easily add expenses and view detailed statistics.
- **Secure Data Handling**: Persistent local storage with backup support.
- **Intuitive UI**: Designed with user-friendly interfaces and advanced customizations.

---

## Requirements
- **Android Version**: API Level 31 or higher.
- **Permissions**:
  - Network access (`INTERNET`, `ACCESS_NETWORK_STATE`)
  - SMS permissions (`RECEIVE_SMS`, `READ_SMS`)
  - Notification permissions (`POST_NOTIFICATIONS`)
  - Storage permissions (`READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`)
  - Camera (`CAMERA`)

## Architecture
This project follows the **MVVM** architecture:
- **Model**: Defines application data structures and business logic.
- **View**: Handles UI rendering and user interactions.
- **ViewModel**: Acts as a bridge between Model and View, managing UI-related logic and data binding.

---

## Folder Structure
expense_module/ 
├── core/ # Application initialization and entry points 
├── data/ # Data-related components │ 
├── model/ # Data models │ 
├── repository/ # Repository layer for data operations │ 
├── source/ # Data sources (local and network) │ 
├── provider/ # System and external providers 
├── ui/ # User interface components │ 
├── adapter/ # RecyclerView and Spinner adapters │ 
├── view/ # Activities, Fragments, and custom Views │ 
├── viewmodel/ # ViewModels for UI components 
├── util/ # Utility and helper classes


---

## Installation
1. Clone this repository:
   ```bash
   git clone https://github.com/your-username/expense-module.git
2. Open the project in Android Studio.
3. Sync Gradle files and build the project.
4. Run the application on an emulator or physical device.

AndroidManifest Highlights
   * Main activity: .expense_module.core.MainActivity
   * Custom receivers for SMS actions:
   * SmsReceiver
   * SmsActionReceiver
   * Services:
   * SmsJobService
   * NotificationListener

Permissions
This app requires the following permissions:
   - SMS: To parse expense-related SMS.
   - Storage: To save and load backup data.
   - Network: For currency conversion and API calls.

Contribution
Contributions are welcome! Feel free to fork the repository, create issues, or submit pull requests for improvements or new features.

License
This project is licensed under the MIT License.

Developer Notes
Ensure compliance with Android's runtime permissions for a seamless user experience.
Follow MVVM principles to maintain a clean and modular architecture.
