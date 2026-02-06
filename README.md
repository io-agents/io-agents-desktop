# io-agents-desktop

Aplikacja desktopowa (Kotlin Multiplatform, JVM) do generacji modeli opisu oprogramowania z użyciem agentów AI — diagramy przypadków użycia, PlantUML, scenariusze, diagramy aktywności.

---

## Wymagania do uruchomienia

- **JDK 17+**
- **Gradle** (używany wrapper z projektu: `./gradlew`)
- W zależności od wybranego dostawcy modelu (patrz niżej):
  - **Ollama** — zainstalowana i uruchomiona lokalna instancja [Ollama](https://ollama.com), z pobranym modelem
  - **Google** — klucz API z [Google AI Studio](https://aistudio.google.com/app/api-keys)

---

## Konfiguracja modelu i tokenu

Wszystkie ustawienia modelu i dostawcy są w jednym pliku:

**`composeApp/src/jvmMain/kotlin/com/pawlowski/io_agents_desktop/config/LLMConfig.kt`**

### 1. Wybór dostawcy (Ollama lub Google)

W pliku `LLMConfig.kt` ustaw:

```kotlin
val provider: Provider = Provider.OLLAMA   // albo Provider.GOOGLE
```

### 2. Konfiguracja dla **Ollama** (gdy `provider == OLLAMA`)

- **OLLAMA_BASE_URL** — adres lokalnej Ollama (domyślnie `http://localhost:11434`).
- **OLLAMA_MODEL_ID** — nazwa modelu w Ollama, np. `"llama3.1:8b"`, `"llama3.2"`, `"mistral"`.  
  Listę modeli zobaczysz po `ollama list` w terminalu.

**Wymagania:**

- Ollama musi być uruchomiona (np. `ollama serve` lub aplikacja desktopowa).
- Wybrany model musi być pobrany, np. `ollama pull llama3.1:8b`.

**Token:** przy Ollama **nie jest wymagany** żaden token ani zmienna środowiskowa.

### 3. Konfiguracja dla **Google** (gdy `provider == GOOGLE`)

- **GOOGLE_MODEL** — identyfikator modelu, np. Gemini2_0Flash, Gemini2_0Flash001, Gemini2_0FlashLite, Gemini2_5Pro, Gemini2_5Flash, Gemini2_5FlashLite

**Wymagania:**

- Klucz API z [Google AI Studio](https://aistudio.google.com/app/api-keys).

**Token autoryzacyjny** — podaj klucz przez **zmienną środowiskową** (nazwa z `LLMConfig.API_KEY_ENV_VAR`, domyślnie `GOOGLE_API_KEY`):

- **Linux / macOS (terminal):**
  ```bash
  export GOOGLE_API_KEY="twój-klucz-api"
  ./gradlew :composeApp:run
  ```

- **Linux / macOS (jednorazowo w tej samej sesji):**
  ```bash
  GOOGLE_API_KEY="twój-klucz-api" ./gradlew :composeApp:run
  ```

- **Windows (PowerShell):**
  ```powershell
  $env:GOOGLE_API_KEY = "twój-klucz-api"
  .\gradlew.bat :composeApp:run
  ```

- **W IDE (IntelliJ / Cursor):**  
  Run → Edit Configurations → wybrana konfiguracja → **Environment variables** → dodaj `GOOGLE_API_KEY=twój-klucz-api`.


---

## Podsumowanie: co musi być spełnione, żeby odpalić projekt

| Dostawca | Wymagania | Token |
|----------|-----------|--------|
| **Ollama** | Ollama zainstalowana i uruchomiona, wybrany model pobrany (`ollama pull <model>`) | Nie wymagany |
| **Google** | Konto Google, klucz API z Google AI Studio | Wymagany: zmienna środowiskowa `GOOGLE_API_KEY` |

Dla wybranego dostawcy w `LLMConfig.kt` ustaw `provider` oraz ewentualnie `OLLAMA_MODEL_ID` / `googleModel`. Dla Google ustaw `GOOGLE_API_KEY` przed uruchomieniem aplikacji.

---

## Build i uruchomienie (JVM)

- **macOS / Linux:**
  ```bash
  ./gradlew :composeApp:run
  ```
- **Windows:**
  ```bash
  .\gradlew.bat :composeApp:run
  ```

Możesz też użyć konfiguracji uruchomienia z widżetu Run w IDE.
