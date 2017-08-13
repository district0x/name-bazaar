pragma solidity ^0.4.14;

import "Offering.sol";
import "SafeMath.sol";
import "EnglishAuctionOfferingLibrary.sol";

contract EnglishAuctionOffering is Offering {
    using EnglishAuctionOfferingLibrary for EnglishAuctionOfferingLibrary.EnglishAuctionOffering;

    EnglishAuctionOfferingLibrary.EnglishAuctionOffering public englishAuctionOffering;

    function EnglishAuctionOffering(
        address _offeringRegistry,
        address _registrar,
        bytes32 _node,
        string _name,
        bytes32 _labelHash,
        address _originalOwner,
        address _emergencyMultisig,
        uint _startPrice,
        uint _endTime,
        uint _extensionDuration,
        uint _minBidIncrease
    )
        Offering(_offeringRegistry, _registrar, _node, _name, _labelHash,  _originalOwner, _emergencyMultisig, 100000, _startPrice)
    {
        englishAuctionOffering.construct(
            _endTime,
            _extensionDuration,
            _minBidIncrease
        );
    }

    function bid() payable {
        englishAuctionOffering.bid(offering);
    }

    function finalize(bool transferPrice) {
        englishAuctionOffering.finalize(offering, transferPrice);
    }

    function withdraw(address _address) {
        englishAuctionOffering.withdraw(offering, _address);
    }

    function setSettings(
        uint _startPrice,
        uint _endTime,
        uint _extensionDuration,
        uint _minBidIncrease
    ) {
        englishAuctionOffering.setSettings(
            offering,
            _startPrice,
            _endTime,
            _extensionDuration,
            _minBidIncrease);
    }

    function reclaimOwnership() {
        englishAuctionOffering.reclaimOwnership(offering);
    }

    function() payable {
        bid();
    }
}

