package models

import securesocial.core.BasicProfile

// a simple User class that can have multiple identities
case class Project(id: String, name: String, icon: String, gitUrl: String, version: String)

case class User(id: String,
                login: String,
                fullname: String,
                email: String,
                wantNewsletter: Boolean,
                projects: List[Project],
                main: BasicProfile,
                identities: List[BasicProfile])