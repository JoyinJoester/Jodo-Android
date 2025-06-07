# Jodo - Android Todo Application

[![中文版](https://img.shields.io/badge/README-中文版-blue)](README_ZH.md)

## Project Overview

Jodo is a feature-rich Android todo application designed to provide a clean and efficient task management experience.

## Key Features

### Task Management
- ✅ Add, edit, and delete tasks
- ✅ Mark task completion status
- ✅ Star important tasks
- ✅ Set task due dates
- ✅ Task reminder notifications
- ✅ Recurring task settings

### Category Management
- ✅ Create custom categories
- ✅ Drag-and-drop category sorting
- ✅ Category color coding
- ✅ Filter tasks by category

### Filtering Options
- ✅ All tasks
- ✅ Active tasks
- ✅ Completed tasks
- ✅ Starred tasks
- ✅ Today's tasks
- ✅ Filter by category

### Statistics
- ✅ Task completion statistics
- ✅ Category task count statistics
- ✅ Visual chart displays

### User Interface
- ✅ Material Design standards
- ✅ Dark/Light theme support
- ✅ Intuitive sidebar navigation
- ✅ Smooth animations
- ✅ Responsive layout

## Technical Highlights

### Architecture
- **MVVM Pattern** - Using ViewModel and LiveData
- **Repository Pattern** - Data access layer abstraction
- **Single Source of Truth** - StateFlow unified state management

### Technology Stack
- **Kotlin** - 100% Kotlin development
- **Android Jetpack** - ViewModel, LiveData, Navigation, etc.
- **Material Design Components** - Modern UI components
- **Coroutines** - Asynchronous programming
- **Gson** - JSON serialization
- **WorkManager** - Background task scheduling

### Data Storage
- **Local JSON Storage** - Lightweight data persistence
- **Type Safety** - Strongly typed data models
- **Data Migration** - Version compatibility support

## Project Structure

```
app/src/main/java/takagicom/todo/jodo/
├── adapter/           # RecyclerView adapters
├── model/            # Data models
├── repository/       # Data repositories
├── service/          # Background services
├── ui/              # UI components
├── utils/           # Utility classes
├── viewmodel/       # ViewModel layer
└── MainActivity.kt  # Main activity
```

## System Requirements

- **Minimum Android Version**: Android 7.0 (API 24)
- **Target Android Version**: Android 14 (API 34)
- **Gradle Version**: 8.7
- **Kotlin Version**: 1.9.22

## Build and Installation

1. Clone the repository
```bash
git clone https://github.com/JoyinJoester/Jodo-Android.git
cd Jodo-Android
```

2. Open the project in Android Studio

3. Build the project
```bash
./gradlew assembleDebug
```

4. Install on device
```bash
./gradlew installDebug
```

## Screenshots

*Screenshots to be added*

## Version History

### v1.0.0 (Current Version)
- Basic task management functionality
- Category system
- Filtering capabilities
- Reminder functionality
- Recurring tasks
- Statistics features

## Contributing

Issues and Pull Requests are welcome to improve this project!

### How to Contribute
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the GPL License - see the [LICENSE](LICENSE) file for details

## Contact

If you have any questions or suggestions, please contact us through:
- Submit a GitHub Issue
- Send email to: [joyin8888@foxmail.com]

## Acknowledgments

- Thanks to all contributors who helped make this project better
- Special thanks to the Android development community for their valuable resources and libraries

---

**Note**: This is an open-source project created for learning and demonstration purposes. Feel free to use, modify, and distribute according to the GPL license terms.
