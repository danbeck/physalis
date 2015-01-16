package models

import securesocial.core.BasicProfile

// a simple User class that can have multiple identities
case class DemoUser(main: BasicProfile, identities: List[BasicProfile])