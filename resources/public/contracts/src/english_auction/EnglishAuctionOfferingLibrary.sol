pragma solidity ^0.4.14;

import "SafeMath.sol";
import "OfferingLibrary.sol";

library EnglishAuctionOfferingLibrary {
    using SafeMath for uint;
    using OfferingLibrary for OfferingLibrary.Offering;

    struct EnglishAuctionOffering {
        uint  endTime;
        uint  extensionDuration;
        uint  minBidIncrease;
        address winningBidder;
        mapping(address => uint) pendingReturns;
    }

    event onBid(address indexed bidder, uint amount, uint endTime, uint datetime);

    function construct(
        EnglishAuctionOffering storage self,
        uint _endTime,
        uint _extensionDuration,
        uint _minBidIncrease
    ) {
        require(_endTime > now);
        self.endTime = _endTime;
        self.extensionDuration = _extensionDuration;
        require(_minBidIncrease > 0);
        self.minBidIncrease = _minBidIncrease;
    }

    function bid(
        EnglishAuctionOffering storage self,
        OfferingLibrary.Offering storage offering
    ) {
        require(now < self.endTime);
        require(msg.sender != self.winningBidder);

        uint bidValue = self.pendingReturns[msg.sender].add(msg.value);
        self.pendingReturns[msg.sender] = 0;

        if (self.winningBidder == 0x0) {
            require(bidValue >= offering.price);
        } else {
            require(bidValue >= offering.price.add(self.minBidIncrease));
            self.pendingReturns[self.winningBidder] = self.pendingReturns[self.winningBidder].add(offering.price);
        }

        self.winningBidder = msg.sender;
        offering.price = bidValue;

        if ((self.endTime - self.extensionDuration) <= now) {
            self.endTime = now.add(self.extensionDuration);
        }

        onBid(msg.sender, offering.price, self.endTime, now);
        offering.setChanged();
    }

    function withdraw(
        EnglishAuctionOffering storage self,
        OfferingLibrary.Offering storage offering,
        address _address
    ) {
        require(msg.sender == _address || offering.isSenderEmergencyMultisig());
        if (self.pendingReturns[_address] > 0) {
            self.pendingReturns[_address] = 0;
            _address.transfer(self.pendingReturns[_address]);
        }
    }

    function finalize(
        EnglishAuctionOffering storage self,
        OfferingLibrary.Offering storage offering,
        bool transferPrice
    ) {
        require(now > self.endTime);
        require(self.winningBidder != 0x0);
        offering.transferOwnership(self.winningBidder, offering.price);

        if (transferPrice) {
            offering.originalOwner.transfer(offering.price);
        } else {
            self.pendingReturns[offering.originalOwner] =
                self.pendingReturns[offering.originalOwner].add(offering.price);
        }
    }

    function reclaimOwnership(
        EnglishAuctionOffering storage self,
        OfferingLibrary.Offering storage offering
    ) {
        if (offering.isSenderEmergencyMultisig()) {
            if (!hasNoBids(self) && !offering.wasEmergencyCancelled()) {
                self.pendingReturns[self.winningBidder] = offering.price;
            }
        } else {
            require(hasNoBids(self));
        }
        offering.reclaimOwnership();
    }

    function setSettings(
        EnglishAuctionOffering storage self,
        OfferingLibrary.Offering storage offering,
        uint _startPrice,
        uint _endTime,
        uint _extensionDuration,
        uint _minBidIncrease
    ) {
        require(offering.isSenderOriginalOwner());
        require(hasNoBids(self));
        offering.price = _startPrice;

        construct(
            self,
            _endTime,
            _extensionDuration,
            _minBidIncrease
        );
        offering.setChanged();
    }

    function hasNoBids(EnglishAuctionOffering storage self) returns(bool) {
        return self.winningBidder == 0x0;
    }
}

