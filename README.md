<h1>AskUp</h1>

<h2>Description</h2>
AskUp is an Android application designed to improve classroom engagement by providing
students with a digital platform to ask questions during lectures. The application addresses a
documented problem where over 70% of university students feel too anxious to ask questions in
front of their peers. By offering an anonymous digital alternative, AskUp encourages
participation from students who might otherwise remain silent.
The system operates on a session based model where lecturers create classroom sessions
using simple 5 digit codes. Students join these sessions by entering the code provided by their
lecturer. Once connected to a session, students can post questions, upvote questions from
other students, and view answers provided by the lecturer. Lecturers can see all questions from
their session, mark them as answered, pin important questions to highlight them, and provide
detailed responses.
The technical implementation uses a local Room database for data persistence, ensuring the
application works offline and maintains fast response times. All user interface components
follow Material Design 3 guidelines to provide a modern and familiar Android experience. The
application includes accessibility features such as dark mode support and speech to text input
for posting questions.

Design requirements emphasize:
- Neat, consistent layouts across all screens with support for light and dark themes;
- Robust input validation (login, registration, 5-digit session code, question text);
- Proper local database integration using Room with reactive updates and usability features
  such as ordered question lists (pinned + most upvoted first), swipe-to-upvote, and
  clear status indicators (answered / pending);
- Accessibility and usability considerations, including large touch targets, speech-to-text
  question input, and clear feedback (vibration, toasts, and notifications).
<br />


<h2>Languages and Utilities Used</h2>

- <b>Kotlin</b> (Jetpack Compose UI)
- <b>Android SDK</b> & <b>Android Studio</b>
- <b>Room</b> Persistence Library (local SQLite database)
- <b>Kotlin Coroutines</b> for asynchronous database access
- <b>SharedPreferences</b> for session persistence
- <b>Google Play Services FusedLocationProviderClient</b> (GPS / city detection)
- <b>Android Speech Recognizer</b> (voice-to-text question input)
- <b>Android Notification APIs</b> (local notifications)
- <b>Git / GitHub</b> for version control

<h2>Screenshots</h2>

![image](https://github.com/user-attachments/assets/2fb3e434-1b55-48c5-8af7-a066f92d66f4)
![image](https://github.com/user-attachments/assets/3d853e9f-193c-43dc-8744-2e7fbbe8799b)
![image](https://github.com/user-attachments/assets/8e26db56-b390-4688-b4de-974f07d3b40b)
![image](https://github.com/user-attachments/assets/43ddb607-8a08-474f-b084-b927ee28074f)
![image](https://github.com/user-attachments/assets/103a6312-4c42-4408-8b3d-551594dd672b)
![image](https://github.com/user-attachments/assets/1e6551e5-8076-4a74-9a69-0274e6a254b3)
