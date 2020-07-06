import io.estatico.newtype.macros.newsubtype
import io.estatico.newtype.ops._

package object binary {

  /**
    * Opcode number in tftp header, that represents packet type
    */
  @newsubtype class Opcode private (val code: Short)
  object Opcode {
    def validate(value: Opcode): Option[Opcode] = Option.when(value.code < 6)(value)
    def unsafe(value: Short): Opcode            = value.coerce
  }

  /**
    * Data block number in Data and ACK packets
    */
  @newsubtype case class Block(number: Short)

  /**
    * Error code in Error packet
    */
  @newsubtype case class ErrorCode(code: Short)
}
