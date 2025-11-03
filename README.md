# Task Assistant

> An Android mobile application to help motivate children and teenagers to complete chores through a point-based reward system and full parental control.

Task Assistant is an application designed for families who want to introduce healthy habits and rules. Children earn points for completing daily chores (e.g., cleaning, homework) and can then exchange them for rewards (e.g., console gaming time).

The parent acts as an administrator—creating tasks, approving their completion, awarding points, and managing the list of available rewards.

---

##  Core Features

The project is based on two main user panels with different permissions.

###  Parent Panel (Admin)
* **Family Management:** Add and view child profiles.
* **Task Creation:** Define tasks and assign point values (e.g., "Clean room" - 100 pts).
* **Reward Creation:** Define rewards and their "price" (e.g., "1 hour of gaming" - 500 pts).
* **Verification:** A panel to approve or reject tasks submitted by children (including a proof preview, e.g., a photo).

###  Child Panel (User)
* **Task List:** View the list of available tasks to complete.
* **Task Submission:** Mark tasks as "done" and submit proof (e.g., a photo) for parental approval.
* **Task Status:** Track the status of submitted tasks (Pending, Approved, Rejected).
* **Reward Shop:** Browse available rewards and "purchase" them with earned points.

---

##  Technology Stack

This application is built using the modern, Google-recommended technology stack for Android development.

* **Language:** Kotlin
* **UI Toolkit:** Jetpack Compose
* **Backend & Database:** Firebase (Authentication, Firestore, Storage)
* **Architecture:** MVVM (Model-View-ViewModel)

