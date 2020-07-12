import binary.Tftp.{ErrorCode, Undefined}
import cats.data.Validated
import io.estatico.newtype.macros.newsubtype
import io.estatico.newtype.ops._

package object binary {

  /**
    * Opcode number in tftp header, that represents packet type
    */
  @newsubtype class Opcode private (val code: Short)
  object Opcode {
    def validate(value: Opcode): Validated[ErrorCode, Opcode] =
      Validated.cond(value.code < 6, value, Undefined("wrong opcode number"))
    def unsafe(value: Short): Opcode = value.coerce
  }

  /**
    * Data block number in Data and ACK packets
    */
  @newsubtype case class Block(number: Short)

  /**
    * Error code in Error packet
    */
  @newsubtype case class Code(code: Short)
}
