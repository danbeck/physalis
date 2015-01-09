package models

case class User(val email: String) {
  def validated: Boolean = false
}

object User {
  def authenticate(email: String, password: String): User = {
    User(email)
  }

  def findByEmail(email: String) = User(email)
}