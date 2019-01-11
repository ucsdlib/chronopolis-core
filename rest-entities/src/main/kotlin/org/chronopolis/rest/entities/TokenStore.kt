package org.chronopolis.rest.entities

import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity
@DiscriminatorValue("TOKEN_STORE")
class TokenStore : DataFile()