// SPDX-License-Identifier: MIT
pragma solidity ^0.8.4;

import "./DelegateProxy.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

/**
 * @title Forwarder proxy contract with editable target
 *
 * @dev For OfferingRegistry contract we use mutable forwarder instead of using the contract directly.
 * This is for better upgradeability. Since OfferingRegistry fires all events related to offerings,
 * we want to be able to access whole history of events always on the same address. Which would be the
 * address of a MutableForwarder. When OfferingRegistry contract is replaced with an updated one,
 * mutable forwarder just replaces target and all events stay still accessible on the same address.
 */
contract MutableForwarder is DelegateProxy, Ownable {
    address public target = 0xBEeFbeefbEefbeEFbeEfbEEfBEeFbeEfBeEfBeef; // checksummed to silence warning

    /**
     * @dev Replaces target forwarder contract is pointing to
     * Only owner can replace target

     * @param _target New target to proxy into
    */
    function setTarget(address _target) public onlyOwner {
        target = _target;
    }

    fallback() external payable {
        delegatedFwd(target, msg.data);
    }
}
