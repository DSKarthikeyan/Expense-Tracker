# Expense-Tracker

The **Expense Module** is an Android application designed to manage and track expenses seamlessly. It leverages modern development practices, including the **MVVM (Model-View-ViewModel)** architecture, ensuring a clean separation of concerns and maintainable code.

## Features
- **Expense Tracking**: Automatically capture expense details from SMS notifications.
- **Add and Manage Expenses**: Easily add expenses and view detailed statistics.
- **Secure Data Handling**: Persistent local storage with backup support.
- **Intuitive UI**: Designed with user-friendly interfaces and advanced customizations.

---

## Architecture
This project follows the **MVVM** architecture:
- **Model**: Defines application data structures and business logic.
- **View**: Handles UI rendering and user interactions.
- **ViewModel**: Acts as a bridge between Model and View, managing UI-related logic and data binding.

---

## Folder Structure
expense_module/ ├── core/ # Application initialization and entry points ├── data/ # Data-related components │ ├── model/ # Data models │ ├── repository/ # Repository layer for data operations │ ├── source/ # Data sources (local and network) │ ├── provider/ # System and external providers ├── ui/ # User interface components │ ├── adapter/ # RecyclerView and Spinner adapters │ ├── view/ # Activities, Fragments, and custom Views │ ├── viewmodel/ # ViewModels for UI components ├── util/ # Utility and helper classes
