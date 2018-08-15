package org.chronopolis.rest.models.update

data class PasswordUpdate(var oldPassword: String,
                          var newPassword: String)
