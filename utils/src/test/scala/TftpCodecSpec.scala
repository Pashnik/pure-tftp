import java.nio.charset.StandardCharsets

import binary.Block
import binary.Tftp.{Acknowledgment, Data, RRQ, WRQ}
import org.scalacheck._
import org.scalacheck.Prop._
import cats.syntax.validated._
import binary.Codec._
import io.estatico.newtype.ops._
import fs2.Chunk
import org.scalatest.matchers.should.Matchers

object TftpCodecSpec extends Properties("TftpCodec") with Matchers {
  def roundTripLaw[T: Codec](value: T) =
    (Encoder[T].encode _ andThen Decoder[T].decode)(value) == value.valid

  val strGen   = Gen.alphaNumStr
  val shortGen = Arbitrary.arbitrary[Short]

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

  property("RRQ-packet") = forAll { rrq: RRQ =>
    roundTripLaw(rrq)
  }

  property("WRQ-packet") = forAll { wrq: WRQ =>
    roundTripLaw(wrq)
  }

  property("Data-packet") = forAll { data: Data =>
    roundTripLaw(data)
  }

  property("Ack-packet") = forAll { packet: Acknowledgment =>
    roundTripLaw(packet)
  }
}
