pragma solidity ^0.4.18;

/**
 * @dev This file is stripped version of Aragon's ForwarderFactory.sol (https://github.com/aragon/apm-contracts/blob/master/contracts/ForwarderFactory.sol)
 */

contract DelegateProxy {
    /**
    * @dev Performs a delegatecall and returns whatever the delegatecall returned (entire context execution will return!)
    * @param _dst Destination address to perform the delegatecall
    * @param _calldata Calldata for the delegatecall
    */
    function delegatedFwd(address _dst, bytes _calldata) internal {
        assembly {
        switch extcodesize(_dst) case 0 { revert(0, 0) }
        let len := 4096
        let result := delegatecall(sub(gas, 10000), _dst, add(_calldata, 0x20), mload(_calldata), 0, len)
        switch result case 0 { invalid() }
        return (0, len)
        }
    }
}

contract Forwarder is DelegateProxy {
    // After compiling contract, `beefbeef...` is replaced in the bytecode by the real target address
    address constant target = 0xBEeFbeefbEefbeEFbeEfbEEfBEeFbeEfBeEfBeef; // checksumed to silence warning

    /*
    * @dev Forwards all calls to target
    */
    function() payable {
        delegatedFwd(target, msg.data);
    }
}
