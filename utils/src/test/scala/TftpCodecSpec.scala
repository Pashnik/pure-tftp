import java.nio.charset.StandardCharsets

import binary.Block
import binary.Tftp._
import org.scalacheck._
import org.scalacheck.Prop.{forAll, propBoolean}
import cats.syntax.validated._
import binary.Codec._
import io.estatico.newtype.ops._
import fs2.Chunk
import org.scalatest.matchers.should.Matchers

object TftpCodecSpec extends Properties("TftpCodec") with Matchers {
  def roundTripLaw[T: Codec](value: T) =
    (Encoder[T].encode _ andThen Decoder[T].decode)(value) == value.valid

  val strGen   = Gen.alphaNumStr
  val shortGen = Gen.choose(0, 25000).map(_.toShort)

  implicit val rrqArbitrary = Arbitrary(strGen.map(RRQ(_)))
  implicit val wrqArbitrary = Arbitrary(strGen.map(WRQ(_)))
  implicit val dataArbitrary =
    Arbitrary {
      for {
        data   <- strGen
        opcode <- shortGen
      } yield
        Data(
            opcode.coerce[Block]
          , Chunk.array(data.getBytes(StandardCharsets.UTF_8))
        )
    }
  implicit val packetArbitrary = Arbitrary(shortGen.map(s => Acknowledgment(s.coerce[Block])))
  implicit val errArbitrary =
    Arbitrary {
      for {
        toErrCode <- Gen.oneOf(
                        Undefined(_)
                      , FileNotFound(_)
                      , IllegalOperator(_)
                      , FileAlreadyExists(_)
                    )
        str <- strGen
      } yield toErrCode(str)
    }

  property("RRQ-packet") = forAll { rrq: RRQ =>
    roundTripLaw(rrq)
  }

  property("WRQ-packet") = forAll { wrq: WRQ =>
    roundTripLaw(wrq)
  }

  property("Data-packet") = forAll { data: Data =>
    (data.raw.size <= 512) ==> roundTripLaw(data)
  }

  property("Ack-packet") = forAll { packet: Acknowledgment =>
    roundTripLaw(packet)
  }

  property("Err-packet") = forAll { err: ErrorCode =>
    roundTripLaw(err)
  }
}
