package utils

object StringUtils {

  def generateUuid(): String = java.util.UUID.randomUUID().toString

}
