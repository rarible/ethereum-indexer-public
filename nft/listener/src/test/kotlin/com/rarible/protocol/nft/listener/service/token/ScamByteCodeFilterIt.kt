package com.rarible.protocol.nft.listener.service.token

import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.service.token.filter.ScamByteCodeFilter
import com.rarible.protocol.nft.listener.test.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.test.IntegrationTest
import io.daonomic.rpc.domain.Binary
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@IntegrationTest
internal class ScamByteCodeFilterIt : AbstractIntegrationTest() {

    @Test
    fun testConfig() = runBlocking<Unit> {
        val filter = ScamByteCodeFilter(
            FeatureFlags(filterScamToken = true),
            nftIndexerProperties.scamByteCodes
        )
        val result = filter.isValid(
            Binary.apply("0x608060405234801561001057600080fd5b50600436106100f45760003560e01c80638da5cb5b11610097578063a6614d6211610066578063a6614d62146101fb578063e985e9c51461020e578063f242432a1461024a578063f2fde38b1461025d57600080fd5b80638da5cb5b146101b2578063927f59ba146101cd57806395d89b41146101e0578063a22cb465146101e857600080fd5b80630e89341c116100d35780630e89341c146101575780632eb2c2d61461016a5780634e1273f41461017f578063570b3c6a1461019f57600080fd5b8062fdd58e146100f957806301ffc9a71461011f57806306fdde0314610142575b600080fd5b61010c610107366004610f8e565b610270565b6040519081526020015b60405180910390f35b61013261012d366004610fd1565b6102cb565b6040519015158152602001610116565b61014a61031c565b6040516101169190611045565b61014a610165366004611058565b6103ae565b61017d6101783660046111bd565b61040c565b005b61019261018d366004611267565b61047b565b604051610116919061136d565b61017d6101ad366004611380565b610542565b6000546040516001600160a01b039091168152602001610116565b61017d6101db3660046113f2565b61057f565b61014a610652565b61017d6101f6366004611455565b610661565b610132610209366004610f8e565b610670565b61013261021c366004611491565b6001600160a01b03918216600090815260026020908152604080832093909416825291909152205460ff1690565b61017d6102583660046114c4565b6106e0565b61017d61026b366004611529565b610748565b60008061027d8484610670565b61028857600061028b565b60015b60008481526001602090815260408083206001600160a01b038916845290915290205460ff9190911691506102c190829061155a565b9150505b92915050565b60006001600160e01b03198216636cdb3d1360e11b14806102fc57506001600160e01b031982166303a24d0760e21b145b806102c557506001600160e01b031982166301ffc9a760e01b1492915050565b60606005805461032b9061156d565b80601f01602080910402602001604051908101604052809291908181526020018280546103579061156d565b80156103a45780601f10610379576101008083540402835291602001916103a4565b820191906000526020600020905b81548152906001019060200180831161038757829003601f168201915b5050505050905090565b60606000600780546103bf9061156d565b9050116103db57604051806020016040528060008152506102c5565b60076103e6836107ce565b6040516020016103f79291906115a7565b60405160208183030381529060405292915050565b6001600160a01b038516331480159061044957506001600160a01b038516600090815260026020908152604080832033845290915290205460ff16155b1561046757604051634cd9539b60e11b815260040160405180910390fd5b6104748585858585610861565b5050505050565b60606000835167ffffffffffffffff81111561049957610499611071565b6040519080825280602002602001820160405280156104c2578160200160208202803683370190505b50905060005b845181101561053a5761050d8582815181106104e6576104e661163e565b60200260200101518583815181106105005761050061163e565b6020026020010151610270565b82828151811061051f5761051f61163e565b602090810291909101015261053381611654565b90506104c8565b509392505050565b6000546001600160a01b0316331461056d576040516330cd747160e01b815260040160405180910390fd5b600761057a8284836116b3565b505050565b6000546001600160a01b031633146105aa576040516330cd747160e01b815260040160405180910390fd5b60005b8181101561057a578282828181106105c7576105c761163e565b90506020020160208101906105dc9190611529565b60085460408051918252600160208301526001600160a01b03929092169160009133917fc3d58168c5ae7397731d063d5bbf3d657854427343f4c083240f7aacaa2d0f62910160405180910390a46008805490600061063a83611654565b9190505550808061064a90611654565b9150506105ad565b60606006805461032b9061156d565b61066c338383610a96565b5050565b600080836001600160a01b03163b11806106a257506001600160a01b03831660009081526003602052604090205460ff165b806106b15750600954600a5410155b806106ca575060008281526004602052604090205460ff165b156106d7575060006102c5565b50600192915050565b6001600160a01b038516331480159061071d57506001600160a01b038516600090815260026020908152604080832033845290915290205460ff16155b1561073b57604051634cd9539b60e11b815260040160405180910390fd5b6104748585858585610b35565b6000546001600160a01b03163314610773576040516330cd747160e01b815260040160405180910390fd5b600080546040516001600160a01b03808516939216917f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e091a3600080546001600160a01b0319166001600160a01b0392909216919091179055565b606060006107db83610c3b565b600101905060008167ffffffffffffffff8111156107fb576107fb611071565b6040519080825280601f01601f191660200182016040528015610825576020820181803683370190505b5090508181016020015b600019016f181899199a1a9b1b9c1cb0b131b232b360811b600a86061a8153600a850494508461082f57509392505050565b60005b8351811015610a30576000610892878684815181106108855761088561163e565b6020026020010151610d13565b905080600160008785815181106108ab576108ab61163e565b602002602001015181526020019081526020016000206000896001600160a01b03166001600160a01b03168152602001908152602001600020546108ef919061155a565b8483815181106109015761090161163e565b6020026020010151111561092857604051631e9acf1760e31b815260040160405180910390fd5b8084838151811061093b5761093b61163e565b6020026020010151036001600087858151811061095a5761095a61163e565b602002602001015181526020019081526020016000206000896001600160a01b03166001600160a01b03168152602001908152602001600020600082825403925050819055508382815181106109b2576109b261163e565b6020026020010151600160008785815181106109d0576109d061163e565b602002602001015181526020019081526020016000206000886001600160a01b03166001600160a01b031681526020019081526020016000206000828254610a18919061155a565b90915550610a299150829050611654565b9050610864565b50836001600160a01b0316856001600160a01b0316336001600160a01b03167f4a39dc06d4c0dbc64b70af90fd698a233a518aa5d07e595d983b8c0526c8f7fb8686604051610a80929190611773565b60405180910390a4610474338686868686610d94565b816001600160a01b0316836001600160a01b031603610ac857604051633cf0df2360e01b815260040160405180910390fd5b6001600160a01b03838116600081815260026020908152604080832094871680845294825291829020805460ff191686151590811790915591519182527f17307eab39ab6107e8899845ad3d59bd9653f200f220920489ca2b5937696c31910160405180910390a3505050565b6000610b418685610d13565b60008581526001602090815260408083206001600160a01b038b168452909152902054909150610b7290829061155a565b831115610b9257604051631e9acf1760e31b815260040160405180910390fd5b60008481526001602090815260408083206001600160a01b038a811685529252808320805485880390039055908716825281208054859290610bd590849061155a565b909155505060408051858152602081018590526001600160a01b03808816929089169133917fc3d58168c5ae7397731d063d5bbf3d657854427343f4c083240f7aacaa2d0f62910160405180910390a4610c33338787878787610eb6565b505050505050565b60008072184f03e93ff9f4daa797ed6e38ed64bf6a1f0160401b8310610c7a5772184f03e93ff9f4daa797ed6e38ed64bf6a1f0160401b830492506040015b6d04ee2d6d415b85acef81000000008310610ca6576d04ee2d6d415b85acef8100000000830492506020015b662386f26fc100008310610cc457662386f26fc10000830492506010015b6305f5e1008310610cdc576305f5e100830492506008015b6127108310610cf057612710830492506004015b60648310610d02576064830492506002015b600a83106102c55760010192915050565b600080610d208484610670565b610d2b576000610d2e565b60015b6001600160a01b03851660009081526003602090815260408083208054600160ff19918216811790925588855260049093529083208054909216179055600a805460ff9390931693508392909190610d8790849061155a565b9091555090949350505050565b6001600160a01b0384163b15610c335760405163bc197c8160e01b81526001600160a01b0385169063bc197c8190610dd890899089908890889088906004016117a1565b6020604051808303816000875af1925050508015610e13575060408051601f3d908101601f19168201909252610e10918101906117ff565b60015b610e7c57610e1f61181c565b806308c379a003610e615750610e33611838565b80610e3e5750610e63565b8060405162461bcd60e51b8152600401610e589190611045565b60405180910390fd5b505b6040516360a54e3360e11b815260040160405180910390fd5b6001600160e01b0319811663bc197c8160e01b14610ead5760405163086d127360e01b815260040160405180910390fd5b50505050505050565b6001600160a01b0384163b15610c335760405163f23a6e6160e01b81526001600160a01b0385169063f23a6e6190610efa90899089908890889088906004016118c2565b6020604051808303816000875af1925050508015610f35575060408051601f3d908101601f19168201909252610f32918101906117ff565b60015b610f4157610e1f61181c565b6001600160e01b0319811663f23a6e6160e01b14610ead5760405163086d127360e01b815260040160405180910390fd5b80356001600160a01b0381168114610f8957600080fd5b919050565b60008060408385031215610fa157600080fd5b610faa83610f72565b946020939093013593505050565b6001600160e01b031981168114610fce57600080fd5b50565b600060208284031215610fe357600080fd5b8135610fee81610fb8565b9392505050565b60005b83811015611010578181015183820152602001610ff8565b50506000910152565b60008151808452611031816020860160208601610ff5565b601f01601f19169290920160200192915050565b602081526000610fee6020830184611019565b60006020828403121561106a57600080fd5b5035919050565b634e487b7160e01b600052604160045260246000fd5b601f8201601f1916810167ffffffffffffffff811182821017156110ad576110ad611071565b6040525050565b600067ffffffffffffffff8211156110ce576110ce611071565b5060051b60200190565b600082601f8301126110e957600080fd5b813560206110f6826110b4565b6040516111038282611087565b83815260059390931b850182019282810191508684111561112357600080fd5b8286015b8481101561113e5780358352918301918301611127565b509695505050505050565b600082601f83011261115a57600080fd5b813567ffffffffffffffff81111561117457611174611071565b60405161118b601f8301601f191660200182611087565b8181528460208386010111156111a057600080fd5b816020850160208301376000918101602001919091529392505050565b600080600080600060a086880312156111d557600080fd5b6111de86610f72565b94506111ec60208701610f72565b9350604086013567ffffffffffffffff8082111561120957600080fd5b61121589838a016110d8565b9450606088013591508082111561122b57600080fd5b61123789838a016110d8565b9350608088013591508082111561124d57600080fd5b5061125a88828901611149565b9150509295509295909350565b6000806040838503121561127a57600080fd5b823567ffffffffffffffff8082111561129257600080fd5b818501915085601f8301126112a657600080fd5b813560206112b3826110b4565b6040516112c08282611087565b83815260059390931b85018201928281019150898411156112e057600080fd5b948201945b83861015611305576112f686610f72565b825294820194908201906112e5565b9650508601359250508082111561131b57600080fd5b50611328858286016110d8565b9150509250929050565b600081518084526020808501945080840160005b8381101561136257815187529582019590820190600101611346565b509495945050505050565b602081526000610fee6020830184611332565b6000806020838503121561139357600080fd5b823567ffffffffffffffff808211156113ab57600080fd5b818501915085601f8301126113bf57600080fd5b8135818111156113ce57600080fd5b8660208285010111156113e057600080fd5b60209290920196919550909350505050565b6000806020838503121561140557600080fd5b823567ffffffffffffffff8082111561141d57600080fd5b818501915085601f83011261143157600080fd5b81358181111561144057600080fd5b8660208260051b85010111156113e057600080fd5b6000806040838503121561146857600080fd5b61147183610f72565b91506020830135801515811461148657600080fd5b809150509250929050565b600080604083850312156114a457600080fd5b6114ad83610f72565b91506114bb60208401610f72565b90509250929050565b600080600080600060a086880312156114dc57600080fd5b6114e586610f72565b94506114f360208701610f72565b93506040860135925060608601359150608086013567ffffffffffffffff81111561151d57600080fd5b61125a88828901611149565b60006020828403121561153b57600080fd5b610fee82610f72565b634e487b7160e01b600052601160045260246000fd5b808201808211156102c5576102c5611544565b600181811c9082168061158157607f821691505b6020821081036115a157634e487b7160e01b600052602260045260246000fd5b50919050565b60008084546115b58161156d565b600182811680156115cd57600181146115e257611611565b60ff1984168752821515830287019450611611565b8860005260208060002060005b858110156116085781548a8201529084019082016115ef565b50505082870194505b505050508351611625818360208801610ff5565b64173539b7b760d91b9101908152600501949350505050565b634e487b7160e01b600052603260045260246000fd5b60006001820161166657611666611544565b5060010190565b601f82111561057a57600081815260208120601f850160051c810160208610156116945750805b601f850160051c820191505b81811015610c33578281556001016116a0565b67ffffffffffffffff8311156116cb576116cb611071565b6116df836116d9835461156d565b8361166d565b6000601f84116001811461171357600085156116fb5750838201355b600019600387901b1c1916600186901b178355610474565b600083815260209020601f19861690835b828110156117445786850135825560209485019460019092019101611724565b50868210156117615760001960f88860031b161c19848701351681555b505060018560011b0183555050505050565b6040815260006117866040830185611332565b82810360208401526117988185611332565b95945050505050565b6001600160a01b0386811682528516602082015260a0604082018190526000906117cd90830186611332565b82810360608401526117df8186611332565b905082810360808401526117f38185611019565b98975050505050505050565b60006020828403121561181157600080fd5b8151610fee81610fb8565b600060033d11156118355760046000803e5060005160e01c5b90565b600060443d10156118465790565b6040516003193d81016004833e81513d67ffffffffffffffff816024840111818411171561187657505050505090565b828501915081518181111561188e5750505050505090565b843d87010160208285010111156118a85750505050505090565b6118b760208286010187611087565b509095945050505050565b6001600160a01b03868116825285166020820152604081018490526060810183905260a0608082018190526000906118fc90830184611019565b97965050505050505056fea2646970667358221220f3879c623b4bfcbfdef6a85899838eb59703d002ad09cead666513280706b42664736f6c63430008120033")
        )
        assertThat(result).isFalse()
    }
}