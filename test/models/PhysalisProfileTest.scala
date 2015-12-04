package models

import collection.mutable.Stack
import org.scalatestplus.play._

class PhysalisProfileTest extends PlaySpec {
  "A BasicProfile " must {
    "have userId == providerUserId" in {
      val expectedValue = "providerUserId"
      val p = PhysalisProfile(providerId = "TestProvideId",
        providerUserId = expectedValue,
        firstName = None,
        lastName = None,
        fullName = None,
        email = None,
        avatarUrl = None)
      p.basicProfile().userId mustBe expectedValue
    }
  }
}