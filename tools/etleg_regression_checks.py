from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]


def method_body(source: str, signature: str) -> str:
    start = source.index(signature)
    opening = source.index("{", start)
    depth = 0
    for index in range(opening, len(source)):
        if source[index] == "{":
            depth += 1
        elif source[index] == "}":
            depth -= 1
            if depth == 0:
                return source[opening + 1:index]
    raise AssertionError(f"Unclosed method: {signature}")


class EtlegRegressionChecks(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.login = (ROOT / "TMessagesProj/src/main/java/org/telegram/ui/LoginActivity.java").read_text()
        cls.settings = (ROOT / "TMessagesProj/src/main/java/org/telegram/ui/SettingsActivity.java").read_text()

    def test_firebase_fallback_keeps_request_guard_until_response(self):
        body = method_body(self.login, "private void resendCodeFromSafetyNet(")
        request = body.index("ConnectionsManager.getInstance(currentAccount).sendRequest")
        self.assertNotIn("isRequestingFirebaseSms = false", body[:request])
        self.assertIn("isRequestingFirebaseSms = false", body[request:])

    def test_firebase_fallback_checks_response_type_without_unsafe_cast(self):
        body = method_body(self.login, "private void resendCodeFromSafetyNet(")
        self.assertIn("response instanceof TLRPC.auth_SentCode", body)
        self.assertNotIn("((TLRPC.auth_SentCode) response).type", body)

    def test_etleg_does_not_request_telegram_only_sms_attestation_or_hash(self):
        body = method_body(self.login, "public void onNextPressed(String code)")
        self.assertIn("settings.allow_firebase = false;", body)
        self.assertIn("settings.allow_app_hash = false;", body)

    def test_etleg_settings_are_reachable_from_settings_screen(self):
        self.assertIn("case 27:", self.settings)
        self.assertIn("presentSettingFragment(new EtlegSettingsActivity())", self.settings)
        self.assertIn("EtlegAppearance.isEtlegApp()", self.settings)
        self.assertIn("EtlegSettingsTitle", self.settings)

    def test_unconfirmed_auth_confirm_removes_only_successful_items_after_responses(self):
        source = (ROOT / "TMessagesProj/src/main/java/org/telegram/messenger/UnconfirmedAuthController.java").read_text()
        body = method_body(source, "private void updateList(boolean confirm,")
        response_results = body.index("if (results[i])")
        removal = body.index("auths.remove(i)")
        callback = body.index("whenDone.run(success)")
        self.assertLess(response_results, removal)
        self.assertLess(removal, callback)
        self.assertNotIn("if (confirm) {\n            final HashSet<Long> hashes", body)

    def test_unconfirmed_auth_confirmation_waits_for_server_result(self):
        source = (ROOT / "TMessagesProj/src/main/java/org/telegram/ui/Cells/UnconfirmedAuthHintCell.java").read_text()
        body = method_body(source, "public void set(final BaseFragment fragment, int currentAccount)")
        click = body[body.index("yesButton.setOnClickListener"):body.index("noButton.setOnClickListener")]
        self.assertIn("yesButton.setLoading(true)", click)
        self.assertIn("if (success == null || success.isEmpty())", click)
        self.assertNotIn(".cleanup()", click)
        self.assertLess(click.index("confirm(auths"), click.index("createSimpleBulletin"))

    def test_preview_build_uses_an_etleg_signing_identity(self):
        workflow = (ROOT / ".github/workflows/etleg-android-preview.yml").read_text()
        self.assertIn('CN=Etleg Preview', workflow)
        self.assertIn('/build-tools/36.0.0/apksigner" verify --verbose --print-certs', workflow)
        self.assertIn('grep -q "Etleg Preview"', workflow)
        self.assertIn('grep -q "Telegram"', workflow)


if __name__ == "__main__":
    unittest.main(verbosity=2)
