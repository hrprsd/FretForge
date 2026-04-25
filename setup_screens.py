import os

base_dir = r"C:\Users\Haraprasad\.gemini\antigravity\Projects\FretForge"

def write(path, content):
    full = os.path.join(base_dir, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8") as f:
        f.write(content)

write("app/src/main/java/com/fretforge/ui/home/TaskLibraryScreen.kt", """package com.fretforge.ui.home

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun TaskLibraryScreen(navController: NavController) {
    Text("Task Library Screen")
}
""")

write("app/src/main/java/com/fretforge/ui/group/GroupReviewScreen.kt", """package com.fretforge.ui.group

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun GroupReviewScreen(navController: NavController, taskIds: String) {
    Text("Group Review Screen: $taskIds")
}
""")

write("app/src/main/java/com/fretforge/ui/practice/PracticeScreen.kt", """package com.fretforge.ui.practice

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun PracticeScreen(navController: NavController) {
    Text("Practice Screen")
}
""")

write("app/src/main/java/com/fretforge/ui/summary/SummaryScreen.kt", """package com.fretforge.ui.summary

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun SummaryScreen(navController: NavController) {
    Text("Summary Screen")
}
""")

write("app/src/main/java/com/fretforge/ui/history/HistoryScreen.kt", """package com.fretforge.ui.history

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun HistoryScreen(navController: NavController) {
    Text("History Screen")
}
""")
print("Screens setup complete.")
