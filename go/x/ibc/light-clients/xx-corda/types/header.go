package types

import "github.com/cosmos/ibc-go/modules/core/exported"

var _ exported.Header = (*Header)(nil)

func (*Header) ClientType() string {
	return CordaClientType
}

func (*Header) GetHeight() exported.Height {
	return cordaHeight
}

func (*Header) ValidateBasic() error {
	return nil
}
