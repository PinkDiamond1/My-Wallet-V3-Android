package info.blockchain.wallet.dynamicselfcustody

import info.blockchain.wallet.bip44.HDAddress
import info.blockchain.wallet.keys.SigningKey
import info.blockchain.wallet.util.PrivateKeyFactory
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation

class DynamicHDAccount(
    params: NetworkParameters,
    wKey: DeterministicKey,
    coinConfig: CoinConfiguration
) {

    private val coinDerivationKey =
        HDKeyDerivation.deriveChildKey(wKey, coinConfig.coinType or ChildNumber.HARDENED_BIT)
    private val accountDerivationKey =
        HDKeyDerivation.deriveChildKey(coinDerivationKey, 0 or ChildNumber.HARDENED_BIT)
    private val addressDerivationKey =
        HDKeyDerivation.deriveChildKey(accountDerivationKey, 0)

    val address = HDAddress(params, addressDerivationKey, 0, coinConfig.purpose)

    val bitcoinSerializedBase58Address: String
        get() = address.formattedAddress

    val signingKey: SigningKey = PrivateKeyFactory().getSigningKey(
        PrivateKeyFactory.WIF_COMPRESSED,
        address.privateKeyString
    )
}
