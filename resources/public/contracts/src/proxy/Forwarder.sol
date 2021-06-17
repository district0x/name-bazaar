// SPDX-License-Identifier: MIT
pragma solidity ^0.8.4;

import "./DelegateProxy.sol";

contract Forwarder is DelegateProxy {
    // After compiling contract, `beefbeef...` is replaced in the bytecode by the real target address
    address constant target = 0xBEeFbeefbEefbeEFbeEfbEEfBEeFbeEfBeEfBeef; // checksummed to silence warning

    /*
    * @dev Forwards all calls to target
    */
    fallback() external payable {
        delegatedFwd(target, msg.data);
    }
}
