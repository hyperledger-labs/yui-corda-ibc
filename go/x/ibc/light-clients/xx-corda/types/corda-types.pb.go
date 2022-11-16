// Code generated by protoc-gen-gogo. DO NOT EDIT.
// source: ibc/lightclients/corda/v1/corda-types.proto

package types

import (
	fmt "fmt"
	proto "github.com/cosmos/gogoproto/proto"
	io "io"
	math "math"
	math_bits "math/bits"
)

// Reference imports to suppress errors if they are not otherwise used.
var _ = proto.Marshal
var _ = fmt.Errorf
var _ = math.Inf

// This is a compile-time assertion to ensure that this generated file
// is compatible with the proto package it is being compiled against.
// A compilation error at this line likely means your copy of the
// proto package needs to be updated.
const _ = proto.GoGoProtoPackageIsVersion3 // please upgrade the proto package

type SecureHash struct {
	Bytes []byte `protobuf:"bytes,1,opt,name=bytes,proto3" json:"bytes,omitempty"`
}

func (m *SecureHash) Reset()         { *m = SecureHash{} }
func (m *SecureHash) String() string { return proto.CompactTextString(m) }
func (*SecureHash) ProtoMessage()    {}
func (*SecureHash) Descriptor() ([]byte, []int) {
	return fileDescriptor_b8c8f64f111266d9, []int{0}
}
func (m *SecureHash) XXX_Unmarshal(b []byte) error {
	return m.Unmarshal(b)
}
func (m *SecureHash) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	if deterministic {
		return xxx_messageInfo_SecureHash.Marshal(b, m, deterministic)
	} else {
		b = b[:cap(b)]
		n, err := m.MarshalToSizedBuffer(b)
		if err != nil {
			return nil, err
		}
		return b[:n], nil
	}
}
func (m *SecureHash) XXX_Merge(src proto.Message) {
	xxx_messageInfo_SecureHash.Merge(m, src)
}
func (m *SecureHash) XXX_Size() int {
	return m.Size()
}
func (m *SecureHash) XXX_DiscardUnknown() {
	xxx_messageInfo_SecureHash.DiscardUnknown(m)
}

var xxx_messageInfo_SecureHash proto.InternalMessageInfo

func (m *SecureHash) GetBytes() []byte {
	if m != nil {
		return m.Bytes
	}
	return nil
}

type StateRef struct {
	Txhash *SecureHash `protobuf:"bytes,1,opt,name=txhash,proto3" json:"txhash,omitempty"`
	Index  uint32      `protobuf:"varint,2,opt,name=index,proto3" json:"index,omitempty"`
}

func (m *StateRef) Reset()         { *m = StateRef{} }
func (m *StateRef) String() string { return proto.CompactTextString(m) }
func (*StateRef) ProtoMessage()    {}
func (*StateRef) Descriptor() ([]byte, []int) {
	return fileDescriptor_b8c8f64f111266d9, []int{1}
}
func (m *StateRef) XXX_Unmarshal(b []byte) error {
	return m.Unmarshal(b)
}
func (m *StateRef) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	if deterministic {
		return xxx_messageInfo_StateRef.Marshal(b, m, deterministic)
	} else {
		b = b[:cap(b)]
		n, err := m.MarshalToSizedBuffer(b)
		if err != nil {
			return nil, err
		}
		return b[:n], nil
	}
}
func (m *StateRef) XXX_Merge(src proto.Message) {
	xxx_messageInfo_StateRef.Merge(m, src)
}
func (m *StateRef) XXX_Size() int {
	return m.Size()
}
func (m *StateRef) XXX_DiscardUnknown() {
	xxx_messageInfo_StateRef.DiscardUnknown(m)
}

var xxx_messageInfo_StateRef proto.InternalMessageInfo

func (m *StateRef) GetTxhash() *SecureHash {
	if m != nil {
		return m.Txhash
	}
	return nil
}

func (m *StateRef) GetIndex() uint32 {
	if m != nil {
		return m.Index
	}
	return 0
}

type CordaX500Name struct {
	CommonName       string `protobuf:"bytes,1,opt,name=common_name,json=commonName,proto3" json:"common_name,omitempty"`
	OrganisationUnit string `protobuf:"bytes,2,opt,name=organisation_unit,json=organisationUnit,proto3" json:"organisation_unit,omitempty"`
	Organisation     string `protobuf:"bytes,3,opt,name=organisation,proto3" json:"organisation,omitempty"`
	Locality         string `protobuf:"bytes,4,opt,name=locality,proto3" json:"locality,omitempty"`
	State            string `protobuf:"bytes,5,opt,name=state,proto3" json:"state,omitempty"`
	Country          string `protobuf:"bytes,6,opt,name=country,proto3" json:"country,omitempty"`
}

func (m *CordaX500Name) Reset()         { *m = CordaX500Name{} }
func (m *CordaX500Name) String() string { return proto.CompactTextString(m) }
func (*CordaX500Name) ProtoMessage()    {}
func (*CordaX500Name) Descriptor() ([]byte, []int) {
	return fileDescriptor_b8c8f64f111266d9, []int{2}
}
func (m *CordaX500Name) XXX_Unmarshal(b []byte) error {
	return m.Unmarshal(b)
}
func (m *CordaX500Name) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	if deterministic {
		return xxx_messageInfo_CordaX500Name.Marshal(b, m, deterministic)
	} else {
		b = b[:cap(b)]
		n, err := m.MarshalToSizedBuffer(b)
		if err != nil {
			return nil, err
		}
		return b[:n], nil
	}
}
func (m *CordaX500Name) XXX_Merge(src proto.Message) {
	xxx_messageInfo_CordaX500Name.Merge(m, src)
}
func (m *CordaX500Name) XXX_Size() int {
	return m.Size()
}
func (m *CordaX500Name) XXX_DiscardUnknown() {
	xxx_messageInfo_CordaX500Name.DiscardUnknown(m)
}

var xxx_messageInfo_CordaX500Name proto.InternalMessageInfo

func (m *CordaX500Name) GetCommonName() string {
	if m != nil {
		return m.CommonName
	}
	return ""
}

func (m *CordaX500Name) GetOrganisationUnit() string {
	if m != nil {
		return m.OrganisationUnit
	}
	return ""
}

func (m *CordaX500Name) GetOrganisation() string {
	if m != nil {
		return m.Organisation
	}
	return ""
}

func (m *CordaX500Name) GetLocality() string {
	if m != nil {
		return m.Locality
	}
	return ""
}

func (m *CordaX500Name) GetState() string {
	if m != nil {
		return m.State
	}
	return ""
}

func (m *CordaX500Name) GetCountry() string {
	if m != nil {
		return m.Country
	}
	return ""
}

type PublicKey struct {
	Encoded []byte `protobuf:"bytes,1,opt,name=encoded,proto3" json:"encoded,omitempty"`
}

func (m *PublicKey) Reset()         { *m = PublicKey{} }
func (m *PublicKey) String() string { return proto.CompactTextString(m) }
func (*PublicKey) ProtoMessage()    {}
func (*PublicKey) Descriptor() ([]byte, []int) {
	return fileDescriptor_b8c8f64f111266d9, []int{3}
}
func (m *PublicKey) XXX_Unmarshal(b []byte) error {
	return m.Unmarshal(b)
}
func (m *PublicKey) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	if deterministic {
		return xxx_messageInfo_PublicKey.Marshal(b, m, deterministic)
	} else {
		b = b[:cap(b)]
		n, err := m.MarshalToSizedBuffer(b)
		if err != nil {
			return nil, err
		}
		return b[:n], nil
	}
}
func (m *PublicKey) XXX_Merge(src proto.Message) {
	xxx_messageInfo_PublicKey.Merge(m, src)
}
func (m *PublicKey) XXX_Size() int {
	return m.Size()
}
func (m *PublicKey) XXX_DiscardUnknown() {
	xxx_messageInfo_PublicKey.DiscardUnknown(m)
}

var xxx_messageInfo_PublicKey proto.InternalMessageInfo

func (m *PublicKey) GetEncoded() []byte {
	if m != nil {
		return m.Encoded
	}
	return nil
}

type Party struct {
	Name      *CordaX500Name `protobuf:"bytes,1,opt,name=name,proto3" json:"name,omitempty"`
	OwningKey *PublicKey     `protobuf:"bytes,2,opt,name=owningKey,proto3" json:"owningKey,omitempty"`
}

func (m *Party) Reset()         { *m = Party{} }
func (m *Party) String() string { return proto.CompactTextString(m) }
func (*Party) ProtoMessage()    {}
func (*Party) Descriptor() ([]byte, []int) {
	return fileDescriptor_b8c8f64f111266d9, []int{4}
}
func (m *Party) XXX_Unmarshal(b []byte) error {
	return m.Unmarshal(b)
}
func (m *Party) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	if deterministic {
		return xxx_messageInfo_Party.Marshal(b, m, deterministic)
	} else {
		b = b[:cap(b)]
		n, err := m.MarshalToSizedBuffer(b)
		if err != nil {
			return nil, err
		}
		return b[:n], nil
	}
}
func (m *Party) XXX_Merge(src proto.Message) {
	xxx_messageInfo_Party.Merge(m, src)
}
func (m *Party) XXX_Size() int {
	return m.Size()
}
func (m *Party) XXX_DiscardUnknown() {
	xxx_messageInfo_Party.DiscardUnknown(m)
}

var xxx_messageInfo_Party proto.InternalMessageInfo

func (m *Party) GetName() *CordaX500Name {
	if m != nil {
		return m.Name
	}
	return nil
}

func (m *Party) GetOwningKey() *PublicKey {
	if m != nil {
		return m.OwningKey
	}
	return nil
}

func init() {
	proto.RegisterType((*SecureHash)(nil), "ibc.lightclients.corda.v1.SecureHash")
	proto.RegisterType((*StateRef)(nil), "ibc.lightclients.corda.v1.StateRef")
	proto.RegisterType((*CordaX500Name)(nil), "ibc.lightclients.corda.v1.CordaX500Name")
	proto.RegisterType((*PublicKey)(nil), "ibc.lightclients.corda.v1.PublicKey")
	proto.RegisterType((*Party)(nil), "ibc.lightclients.corda.v1.Party")
}

func init() {
	proto.RegisterFile("ibc/lightclients/corda/v1/corda-types.proto", fileDescriptor_b8c8f64f111266d9)
}

var fileDescriptor_b8c8f64f111266d9 = []byte{
	// 435 bytes of a gzipped FileDescriptorProto
	0x1f, 0x8b, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0xff, 0x7c, 0x52, 0x41, 0x6b, 0xd4, 0x40,
	0x18, 0xdd, 0x68, 0x77, 0xed, 0x7e, 0xdb, 0x82, 0x0e, 0x1e, 0xa2, 0x87, 0x58, 0x82, 0x85, 0x85,
	0xb2, 0x49, 0xbb, 0xe2, 0x4d, 0x2f, 0xf5, 0x22, 0x14, 0xb4, 0xa4, 0x08, 0xe2, 0x65, 0x99, 0x4c,
	0x3e, 0x93, 0x81, 0x64, 0x66, 0x99, 0x99, 0xd4, 0xcc, 0x4f, 0xf0, 0xe6, 0xcf, 0xf2, 0x22, 0xf4,
	0xe8, 0x51, 0x76, 0xff, 0x88, 0x64, 0xd2, 0x4d, 0xb7, 0x87, 0xee, 0x2d, 0xef, 0xfb, 0xde, 0x7b,
	0xf3, 0xe5, 0xf1, 0xe0, 0x84, 0xa7, 0x2c, 0x2e, 0x79, 0x5e, 0x18, 0x56, 0x72, 0x14, 0x46, 0xc7,
	0x4c, 0xaa, 0x8c, 0xc6, 0xd7, 0x67, 0xdd, 0xc7, 0xcc, 0xd8, 0x25, 0xea, 0x68, 0xa9, 0xa4, 0x91,
	0xe4, 0x05, 0x4f, 0x59, 0xb4, 0x4d, 0x8e, 0x1c, 0x27, 0xba, 0x3e, 0x0b, 0x43, 0x80, 0x2b, 0x64,
	0xb5, 0xc2, 0x8f, 0x54, 0x17, 0xe4, 0x39, 0x0c, 0x53, 0x6b, 0x50, 0xfb, 0xde, 0x91, 0x37, 0x3d,
	0x48, 0x3a, 0x10, 0x2e, 0x60, 0xff, 0xca, 0x50, 0x83, 0x09, 0x7e, 0x27, 0xef, 0x61, 0x64, 0x9a,
	0x82, 0xea, 0xc2, 0x51, 0x26, 0xf3, 0xe3, 0xe8, 0x41, 0xef, 0xe8, 0xce, 0x38, 0xb9, 0x15, 0xb5,
	0x0f, 0x70, 0x91, 0x61, 0xe3, 0x3f, 0x3a, 0xf2, 0xa6, 0x87, 0x49, 0x07, 0xc2, 0x3f, 0x1e, 0x1c,
	0x7e, 0x68, 0x55, 0x5f, 0xdf, 0x9e, 0x9e, 0x7e, 0xa2, 0x15, 0x92, 0x57, 0x30, 0x61, 0xb2, 0xaa,
	0xa4, 0x58, 0x08, 0x5a, 0xa1, 0x7b, 0x6b, 0x9c, 0x40, 0x37, 0x72, 0x84, 0x13, 0x78, 0x26, 0x55,
	0x4e, 0x05, 0xd7, 0xd4, 0x70, 0x29, 0x16, 0xb5, 0xe0, 0xc6, 0x99, 0x8e, 0x93, 0xa7, 0xdb, 0x8b,
	0x2f, 0x82, 0x1b, 0x12, 0xc2, 0xc1, 0xf6, 0xcc, 0x7f, 0xec, 0x78, 0xf7, 0x66, 0xe4, 0x25, 0xec,
	0x97, 0x92, 0xd1, 0x92, 0x1b, 0xeb, 0xef, 0xb9, 0x7d, 0x8f, 0xdb, 0xab, 0x75, 0x1b, 0x80, 0x3f,
	0x74, 0x8b, 0x0e, 0x10, 0x1f, 0x9e, 0x30, 0x59, 0x0b, 0xa3, 0xac, 0x3f, 0x72, 0xf3, 0x0d, 0x0c,
	0x8f, 0x61, 0x7c, 0x59, 0xa7, 0x25, 0x67, 0x17, 0x68, 0x5b, 0x1a, 0x0a, 0x26, 0x33, 0xcc, 0x6e,
	0x53, 0xdd, 0xc0, 0xf0, 0xa7, 0x07, 0xc3, 0x4b, 0xaa, 0x8c, 0x25, 0xef, 0x60, 0xaf, 0xff, 0xcf,
	0xc9, 0x7c, 0xba, 0x23, 0xd3, 0x7b, 0x31, 0x25, 0x4e, 0x45, 0xce, 0x61, 0x2c, 0x7f, 0x08, 0x2e,
	0xf2, 0x0b, 0xb4, 0x2e, 0x83, 0xc9, 0xfc, 0xf5, 0x0e, 0x8b, 0xfe, 0xb4, 0xe4, 0x4e, 0x76, 0xce,
	0x7f, 0xaf, 0x02, 0xef, 0x66, 0x15, 0x78, 0xff, 0x56, 0x81, 0xf7, 0x6b, 0x1d, 0x0c, 0x6e, 0xd6,
	0xc1, 0xe0, 0xef, 0x3a, 0x18, 0x7c, 0xfb, 0x9c, 0x73, 0x53, 0xd4, 0x69, 0xc4, 0x64, 0x15, 0x17,
	0x76, 0x89, 0xaa, 0xc4, 0x2c, 0x47, 0x35, 0x2b, 0x69, 0xaa, 0x63, 0x5b, 0xf3, 0x59, 0xd7, 0xb7,
	0xb6, 0x8f, 0xb9, 0x8c, 0x9b, 0xb8, 0x2f, 0xe6, 0x6c, 0xd3, 0xcc, 0xa6, 0xe9, 0x38, 0xb1, 0xeb,
	0x64, 0x3a, 0x72, 0xa5, 0x7c, 0xf3, 0x3f, 0x00, 0x00, 0xff, 0xff, 0x0b, 0x5c, 0x89, 0xa6, 0xc3,
	0x02, 0x00, 0x00,
}

func (m *SecureHash) Marshal() (dAtA []byte, err error) {
	size := m.Size()
	dAtA = make([]byte, size)
	n, err := m.MarshalToSizedBuffer(dAtA[:size])
	if err != nil {
		return nil, err
	}
	return dAtA[:n], nil
}

func (m *SecureHash) MarshalTo(dAtA []byte) (int, error) {
	size := m.Size()
	return m.MarshalToSizedBuffer(dAtA[:size])
}

func (m *SecureHash) MarshalToSizedBuffer(dAtA []byte) (int, error) {
	i := len(dAtA)
	_ = i
	var l int
	_ = l
	if len(m.Bytes) > 0 {
		i -= len(m.Bytes)
		copy(dAtA[i:], m.Bytes)
		i = encodeVarintCordaTypes(dAtA, i, uint64(len(m.Bytes)))
		i--
		dAtA[i] = 0xa
	}
	return len(dAtA) - i, nil
}

func (m *StateRef) Marshal() (dAtA []byte, err error) {
	size := m.Size()
	dAtA = make([]byte, size)
	n, err := m.MarshalToSizedBuffer(dAtA[:size])
	if err != nil {
		return nil, err
	}
	return dAtA[:n], nil
}

func (m *StateRef) MarshalTo(dAtA []byte) (int, error) {
	size := m.Size()
	return m.MarshalToSizedBuffer(dAtA[:size])
}

func (m *StateRef) MarshalToSizedBuffer(dAtA []byte) (int, error) {
	i := len(dAtA)
	_ = i
	var l int
	_ = l
	if m.Index != 0 {
		i = encodeVarintCordaTypes(dAtA, i, uint64(m.Index))
		i--
		dAtA[i] = 0x10
	}
	if m.Txhash != nil {
		{
			size, err := m.Txhash.MarshalToSizedBuffer(dAtA[:i])
			if err != nil {
				return 0, err
			}
			i -= size
			i = encodeVarintCordaTypes(dAtA, i, uint64(size))
		}
		i--
		dAtA[i] = 0xa
	}
	return len(dAtA) - i, nil
}

func (m *CordaX500Name) Marshal() (dAtA []byte, err error) {
	size := m.Size()
	dAtA = make([]byte, size)
	n, err := m.MarshalToSizedBuffer(dAtA[:size])
	if err != nil {
		return nil, err
	}
	return dAtA[:n], nil
}

func (m *CordaX500Name) MarshalTo(dAtA []byte) (int, error) {
	size := m.Size()
	return m.MarshalToSizedBuffer(dAtA[:size])
}

func (m *CordaX500Name) MarshalToSizedBuffer(dAtA []byte) (int, error) {
	i := len(dAtA)
	_ = i
	var l int
	_ = l
	if len(m.Country) > 0 {
		i -= len(m.Country)
		copy(dAtA[i:], m.Country)
		i = encodeVarintCordaTypes(dAtA, i, uint64(len(m.Country)))
		i--
		dAtA[i] = 0x32
	}
	if len(m.State) > 0 {
		i -= len(m.State)
		copy(dAtA[i:], m.State)
		i = encodeVarintCordaTypes(dAtA, i, uint64(len(m.State)))
		i--
		dAtA[i] = 0x2a
	}
	if len(m.Locality) > 0 {
		i -= len(m.Locality)
		copy(dAtA[i:], m.Locality)
		i = encodeVarintCordaTypes(dAtA, i, uint64(len(m.Locality)))
		i--
		dAtA[i] = 0x22
	}
	if len(m.Organisation) > 0 {
		i -= len(m.Organisation)
		copy(dAtA[i:], m.Organisation)
		i = encodeVarintCordaTypes(dAtA, i, uint64(len(m.Organisation)))
		i--
		dAtA[i] = 0x1a
	}
	if len(m.OrganisationUnit) > 0 {
		i -= len(m.OrganisationUnit)
		copy(dAtA[i:], m.OrganisationUnit)
		i = encodeVarintCordaTypes(dAtA, i, uint64(len(m.OrganisationUnit)))
		i--
		dAtA[i] = 0x12
	}
	if len(m.CommonName) > 0 {
		i -= len(m.CommonName)
		copy(dAtA[i:], m.CommonName)
		i = encodeVarintCordaTypes(dAtA, i, uint64(len(m.CommonName)))
		i--
		dAtA[i] = 0xa
	}
	return len(dAtA) - i, nil
}

func (m *PublicKey) Marshal() (dAtA []byte, err error) {
	size := m.Size()
	dAtA = make([]byte, size)
	n, err := m.MarshalToSizedBuffer(dAtA[:size])
	if err != nil {
		return nil, err
	}
	return dAtA[:n], nil
}

func (m *PublicKey) MarshalTo(dAtA []byte) (int, error) {
	size := m.Size()
	return m.MarshalToSizedBuffer(dAtA[:size])
}

func (m *PublicKey) MarshalToSizedBuffer(dAtA []byte) (int, error) {
	i := len(dAtA)
	_ = i
	var l int
	_ = l
	if len(m.Encoded) > 0 {
		i -= len(m.Encoded)
		copy(dAtA[i:], m.Encoded)
		i = encodeVarintCordaTypes(dAtA, i, uint64(len(m.Encoded)))
		i--
		dAtA[i] = 0xa
	}
	return len(dAtA) - i, nil
}

func (m *Party) Marshal() (dAtA []byte, err error) {
	size := m.Size()
	dAtA = make([]byte, size)
	n, err := m.MarshalToSizedBuffer(dAtA[:size])
	if err != nil {
		return nil, err
	}
	return dAtA[:n], nil
}

func (m *Party) MarshalTo(dAtA []byte) (int, error) {
	size := m.Size()
	return m.MarshalToSizedBuffer(dAtA[:size])
}

func (m *Party) MarshalToSizedBuffer(dAtA []byte) (int, error) {
	i := len(dAtA)
	_ = i
	var l int
	_ = l
	if m.OwningKey != nil {
		{
			size, err := m.OwningKey.MarshalToSizedBuffer(dAtA[:i])
			if err != nil {
				return 0, err
			}
			i -= size
			i = encodeVarintCordaTypes(dAtA, i, uint64(size))
		}
		i--
		dAtA[i] = 0x12
	}
	if m.Name != nil {
		{
			size, err := m.Name.MarshalToSizedBuffer(dAtA[:i])
			if err != nil {
				return 0, err
			}
			i -= size
			i = encodeVarintCordaTypes(dAtA, i, uint64(size))
		}
		i--
		dAtA[i] = 0xa
	}
	return len(dAtA) - i, nil
}

func encodeVarintCordaTypes(dAtA []byte, offset int, v uint64) int {
	offset -= sovCordaTypes(v)
	base := offset
	for v >= 1<<7 {
		dAtA[offset] = uint8(v&0x7f | 0x80)
		v >>= 7
		offset++
	}
	dAtA[offset] = uint8(v)
	return base
}
func (m *SecureHash) Size() (n int) {
	if m == nil {
		return 0
	}
	var l int
	_ = l
	l = len(m.Bytes)
	if l > 0 {
		n += 1 + l + sovCordaTypes(uint64(l))
	}
	return n
}

func (m *StateRef) Size() (n int) {
	if m == nil {
		return 0
	}
	var l int
	_ = l
	if m.Txhash != nil {
		l = m.Txhash.Size()
		n += 1 + l + sovCordaTypes(uint64(l))
	}
	if m.Index != 0 {
		n += 1 + sovCordaTypes(uint64(m.Index))
	}
	return n
}

func (m *CordaX500Name) Size() (n int) {
	if m == nil {
		return 0
	}
	var l int
	_ = l
	l = len(m.CommonName)
	if l > 0 {
		n += 1 + l + sovCordaTypes(uint64(l))
	}
	l = len(m.OrganisationUnit)
	if l > 0 {
		n += 1 + l + sovCordaTypes(uint64(l))
	}
	l = len(m.Organisation)
	if l > 0 {
		n += 1 + l + sovCordaTypes(uint64(l))
	}
	l = len(m.Locality)
	if l > 0 {
		n += 1 + l + sovCordaTypes(uint64(l))
	}
	l = len(m.State)
	if l > 0 {
		n += 1 + l + sovCordaTypes(uint64(l))
	}
	l = len(m.Country)
	if l > 0 {
		n += 1 + l + sovCordaTypes(uint64(l))
	}
	return n
}

func (m *PublicKey) Size() (n int) {
	if m == nil {
		return 0
	}
	var l int
	_ = l
	l = len(m.Encoded)
	if l > 0 {
		n += 1 + l + sovCordaTypes(uint64(l))
	}
	return n
}

func (m *Party) Size() (n int) {
	if m == nil {
		return 0
	}
	var l int
	_ = l
	if m.Name != nil {
		l = m.Name.Size()
		n += 1 + l + sovCordaTypes(uint64(l))
	}
	if m.OwningKey != nil {
		l = m.OwningKey.Size()
		n += 1 + l + sovCordaTypes(uint64(l))
	}
	return n
}

func sovCordaTypes(x uint64) (n int) {
	return (math_bits.Len64(x|1) + 6) / 7
}
func sozCordaTypes(x uint64) (n int) {
	return sovCordaTypes(uint64((x << 1) ^ uint64((int64(x) >> 63))))
}
func (m *SecureHash) Unmarshal(dAtA []byte) error {
	l := len(dAtA)
	iNdEx := 0
	for iNdEx < l {
		preIndex := iNdEx
		var wire uint64
		for shift := uint(0); ; shift += 7 {
			if shift >= 64 {
				return ErrIntOverflowCordaTypes
			}
			if iNdEx >= l {
				return io.ErrUnexpectedEOF
			}
			b := dAtA[iNdEx]
			iNdEx++
			wire |= uint64(b&0x7F) << shift
			if b < 0x80 {
				break
			}
		}
		fieldNum := int32(wire >> 3)
		wireType := int(wire & 0x7)
		if wireType == 4 {
			return fmt.Errorf("proto: SecureHash: wiretype end group for non-group")
		}
		if fieldNum <= 0 {
			return fmt.Errorf("proto: SecureHash: illegal tag %d (wire type %d)", fieldNum, wire)
		}
		switch fieldNum {
		case 1:
			if wireType != 2 {
				return fmt.Errorf("proto: wrong wireType = %d for field Bytes", wireType)
			}
			var byteLen int
			for shift := uint(0); ; shift += 7 {
				if shift >= 64 {
					return ErrIntOverflowCordaTypes
				}
				if iNdEx >= l {
					return io.ErrUnexpectedEOF
				}
				b := dAtA[iNdEx]
				iNdEx++
				byteLen |= int(b&0x7F) << shift
				if b < 0x80 {
					break
				}
			}
			if byteLen < 0 {
				return ErrInvalidLengthCordaTypes
			}
			postIndex := iNdEx + byteLen
			if postIndex < 0 {
				return ErrInvalidLengthCordaTypes
			}
			if postIndex > l {
				return io.ErrUnexpectedEOF
			}
			m.Bytes = append(m.Bytes[:0], dAtA[iNdEx:postIndex]...)
			if m.Bytes == nil {
				m.Bytes = []byte{}
			}
			iNdEx = postIndex
		default:
			iNdEx = preIndex
			skippy, err := skipCordaTypes(dAtA[iNdEx:])
			if err != nil {
				return err
			}
			if (skippy < 0) || (iNdEx+skippy) < 0 {
				return ErrInvalidLengthCordaTypes
			}
			if (iNdEx + skippy) > l {
				return io.ErrUnexpectedEOF
			}
			iNdEx += skippy
		}
	}

	if iNdEx > l {
		return io.ErrUnexpectedEOF
	}
	return nil
}
func (m *StateRef) Unmarshal(dAtA []byte) error {
	l := len(dAtA)
	iNdEx := 0
	for iNdEx < l {
		preIndex := iNdEx
		var wire uint64
		for shift := uint(0); ; shift += 7 {
			if shift >= 64 {
				return ErrIntOverflowCordaTypes
			}
			if iNdEx >= l {
				return io.ErrUnexpectedEOF
			}
			b := dAtA[iNdEx]
			iNdEx++
			wire |= uint64(b&0x7F) << shift
			if b < 0x80 {
				break
			}
		}
		fieldNum := int32(wire >> 3)
		wireType := int(wire & 0x7)
		if wireType == 4 {
			return fmt.Errorf("proto: StateRef: wiretype end group for non-group")
		}
		if fieldNum <= 0 {
			return fmt.Errorf("proto: StateRef: illegal tag %d (wire type %d)", fieldNum, wire)
		}
		switch fieldNum {
		case 1:
			if wireType != 2 {
				return fmt.Errorf("proto: wrong wireType = %d for field Txhash", wireType)
			}
			var msglen int
			for shift := uint(0); ; shift += 7 {
				if shift >= 64 {
					return ErrIntOverflowCordaTypes
				}
				if iNdEx >= l {
					return io.ErrUnexpectedEOF
				}
				b := dAtA[iNdEx]
				iNdEx++
				msglen |= int(b&0x7F) << shift
				if b < 0x80 {
					break
				}
			}
			if msglen < 0 {
				return ErrInvalidLengthCordaTypes
			}
			postIndex := iNdEx + msglen
			if postIndex < 0 {
				return ErrInvalidLengthCordaTypes
			}
			if postIndex > l {
				return io.ErrUnexpectedEOF
			}
			if m.Txhash == nil {
				m.Txhash = &SecureHash{}
			}
			if err := m.Txhash.Unmarshal(dAtA[iNdEx:postIndex]); err != nil {
				return err
			}
			iNdEx = postIndex
		case 2:
			if wireType != 0 {
				return fmt.Errorf("proto: wrong wireType = %d for field Index", wireType)
			}
			m.Index = 0
			for shift := uint(0); ; shift += 7 {
				if shift >= 64 {
					return ErrIntOverflowCordaTypes
				}
				if iNdEx >= l {
					return io.ErrUnexpectedEOF
				}
				b := dAtA[iNdEx]
				iNdEx++
				m.Index |= uint32(b&0x7F) << shift
				if b < 0x80 {
					break
				}
			}
		default:
			iNdEx = preIndex
			skippy, err := skipCordaTypes(dAtA[iNdEx:])
			if err != nil {
				return err
			}
			if (skippy < 0) || (iNdEx+skippy) < 0 {
				return ErrInvalidLengthCordaTypes
			}
			if (iNdEx + skippy) > l {
				return io.ErrUnexpectedEOF
			}
			iNdEx += skippy
		}
	}

	if iNdEx > l {
		return io.ErrUnexpectedEOF
	}
	return nil
}
func (m *CordaX500Name) Unmarshal(dAtA []byte) error {
	l := len(dAtA)
	iNdEx := 0
	for iNdEx < l {
		preIndex := iNdEx
		var wire uint64
		for shift := uint(0); ; shift += 7 {
			if shift >= 64 {
				return ErrIntOverflowCordaTypes
			}
			if iNdEx >= l {
				return io.ErrUnexpectedEOF
			}
			b := dAtA[iNdEx]
			iNdEx++
			wire |= uint64(b&0x7F) << shift
			if b < 0x80 {
				break
			}
		}
		fieldNum := int32(wire >> 3)
		wireType := int(wire & 0x7)
		if wireType == 4 {
			return fmt.Errorf("proto: CordaX500Name: wiretype end group for non-group")
		}
		if fieldNum <= 0 {
			return fmt.Errorf("proto: CordaX500Name: illegal tag %d (wire type %d)", fieldNum, wire)
		}
		switch fieldNum {
		case 1:
			if wireType != 2 {
				return fmt.Errorf("proto: wrong wireType = %d for field CommonName", wireType)
			}
			var stringLen uint64
			for shift := uint(0); ; shift += 7 {
				if shift >= 64 {
					return ErrIntOverflowCordaTypes
				}
				if iNdEx >= l {
					return io.ErrUnexpectedEOF
				}
				b := dAtA[iNdEx]
				iNdEx++
				stringLen |= uint64(b&0x7F) << shift
				if b < 0x80 {
					break
				}
			}
			intStringLen := int(stringLen)
			if intStringLen < 0 {
				return ErrInvalidLengthCordaTypes
			}
			postIndex := iNdEx + intStringLen
			if postIndex < 0 {
				return ErrInvalidLengthCordaTypes
			}
			if postIndex > l {
				return io.ErrUnexpectedEOF
			}
			m.CommonName = string(dAtA[iNdEx:postIndex])
			iNdEx = postIndex
		case 2:
			if wireType != 2 {
				return fmt.Errorf("proto: wrong wireType = %d for field OrganisationUnit", wireType)
			}
			var stringLen uint64
			for shift := uint(0); ; shift += 7 {
				if shift >= 64 {
					return ErrIntOverflowCordaTypes
				}
				if iNdEx >= l {
					return io.ErrUnexpectedEOF
				}
				b := dAtA[iNdEx]
				iNdEx++
				stringLen |= uint64(b&0x7F) << shift
				if b < 0x80 {
					break
				}
			}
			intStringLen := int(stringLen)
			if intStringLen < 0 {
				return ErrInvalidLengthCordaTypes
			}
			postIndex := iNdEx + intStringLen
			if postIndex < 0 {
				return ErrInvalidLengthCordaTypes
			}
			if postIndex > l {
				return io.ErrUnexpectedEOF
			}
			m.OrganisationUnit = string(dAtA[iNdEx:postIndex])
			iNdEx = postIndex
		case 3:
			if wireType != 2 {
				return fmt.Errorf("proto: wrong wireType = %d for field Organisation", wireType)
			}
			var stringLen uint64
			for shift := uint(0); ; shift += 7 {
				if shift >= 64 {
					return ErrIntOverflowCordaTypes
				}
				if iNdEx >= l {
					return io.ErrUnexpectedEOF
				}
				b := dAtA[iNdEx]
				iNdEx++
				stringLen |= uint64(b&0x7F) << shift
				if b < 0x80 {
					break
				}
			}
			intStringLen := int(stringLen)
			if intStringLen < 0 {
				return ErrInvalidLengthCordaTypes
			}
			postIndex := iNdEx + intStringLen
			if postIndex < 0 {
				return ErrInvalidLengthCordaTypes
			}
			if postIndex > l {
				return io.ErrUnexpectedEOF
			}
			m.Organisation = string(dAtA[iNdEx:postIndex])
			iNdEx = postIndex
		case 4:
			if wireType != 2 {
				return fmt.Errorf("proto: wrong wireType = %d for field Locality", wireType)
			}
			var stringLen uint64
			for shift := uint(0); ; shift += 7 {
				if shift >= 64 {
					return ErrIntOverflowCordaTypes
				}
				if iNdEx >= l {
					return io.ErrUnexpectedEOF
				}
				b := dAtA[iNdEx]
				iNdEx++
				stringLen |= uint64(b&0x7F) << shift
				if b < 0x80 {
					break
				}
			}
			intStringLen := int(stringLen)
			if intStringLen < 0 {
				return ErrInvalidLengthCordaTypes
			}
			postIndex := iNdEx + intStringLen
			if postIndex < 0 {
				return ErrInvalidLengthCordaTypes
			}
			if postIndex > l {
				return io.ErrUnexpectedEOF
			}
			m.Locality = string(dAtA[iNdEx:postIndex])
			iNdEx = postIndex
		case 5:
			if wireType != 2 {
				return fmt.Errorf("proto: wrong wireType = %d for field State", wireType)
			}
			var stringLen uint64
			for shift := uint(0); ; shift += 7 {
				if shift >= 64 {
					return ErrIntOverflowCordaTypes
				}
				if iNdEx >= l {
					return io.ErrUnexpectedEOF
				}
				b := dAtA[iNdEx]
				iNdEx++
				stringLen |= uint64(b&0x7F) << shift
				if b < 0x80 {
					break
				}
			}
			intStringLen := int(stringLen)
			if intStringLen < 0 {
				return ErrInvalidLengthCordaTypes
			}
			postIndex := iNdEx + intStringLen
			if postIndex < 0 {
				return ErrInvalidLengthCordaTypes
			}
			if postIndex > l {
				return io.ErrUnexpectedEOF
			}
			m.State = string(dAtA[iNdEx:postIndex])
			iNdEx = postIndex
		case 6:
			if wireType != 2 {
				return fmt.Errorf("proto: wrong wireType = %d for field Country", wireType)
			}
			var stringLen uint64
			for shift := uint(0); ; shift += 7 {
				if shift >= 64 {
					return ErrIntOverflowCordaTypes
				}
				if iNdEx >= l {
					return io.ErrUnexpectedEOF
				}
				b := dAtA[iNdEx]
				iNdEx++
				stringLen |= uint64(b&0x7F) << shift
				if b < 0x80 {
					break
				}
			}
			intStringLen := int(stringLen)
			if intStringLen < 0 {
				return ErrInvalidLengthCordaTypes
			}
			postIndex := iNdEx + intStringLen
			if postIndex < 0 {
				return ErrInvalidLengthCordaTypes
			}
			if postIndex > l {
				return io.ErrUnexpectedEOF
			}
			m.Country = string(dAtA[iNdEx:postIndex])
			iNdEx = postIndex
		default:
			iNdEx = preIndex
			skippy, err := skipCordaTypes(dAtA[iNdEx:])
			if err != nil {
				return err
			}
			if (skippy < 0) || (iNdEx+skippy) < 0 {
				return ErrInvalidLengthCordaTypes
			}
			if (iNdEx + skippy) > l {
				return io.ErrUnexpectedEOF
			}
			iNdEx += skippy
		}
	}

	if iNdEx > l {
		return io.ErrUnexpectedEOF
	}
	return nil
}
func (m *PublicKey) Unmarshal(dAtA []byte) error {
	l := len(dAtA)
	iNdEx := 0
	for iNdEx < l {
		preIndex := iNdEx
		var wire uint64
		for shift := uint(0); ; shift += 7 {
			if shift >= 64 {
				return ErrIntOverflowCordaTypes
			}
			if iNdEx >= l {
				return io.ErrUnexpectedEOF
			}
			b := dAtA[iNdEx]
			iNdEx++
			wire |= uint64(b&0x7F) << shift
			if b < 0x80 {
				break
			}
		}
		fieldNum := int32(wire >> 3)
		wireType := int(wire & 0x7)
		if wireType == 4 {
			return fmt.Errorf("proto: PublicKey: wiretype end group for non-group")
		}
		if fieldNum <= 0 {
			return fmt.Errorf("proto: PublicKey: illegal tag %d (wire type %d)", fieldNum, wire)
		}
		switch fieldNum {
		case 1:
			if wireType != 2 {
				return fmt.Errorf("proto: wrong wireType = %d for field Encoded", wireType)
			}
			var byteLen int
			for shift := uint(0); ; shift += 7 {
				if shift >= 64 {
					return ErrIntOverflowCordaTypes
				}
				if iNdEx >= l {
					return io.ErrUnexpectedEOF
				}
				b := dAtA[iNdEx]
				iNdEx++
				byteLen |= int(b&0x7F) << shift
				if b < 0x80 {
					break
				}
			}
			if byteLen < 0 {
				return ErrInvalidLengthCordaTypes
			}
			postIndex := iNdEx + byteLen
			if postIndex < 0 {
				return ErrInvalidLengthCordaTypes
			}
			if postIndex > l {
				return io.ErrUnexpectedEOF
			}
			m.Encoded = append(m.Encoded[:0], dAtA[iNdEx:postIndex]...)
			if m.Encoded == nil {
				m.Encoded = []byte{}
			}
			iNdEx = postIndex
		default:
			iNdEx = preIndex
			skippy, err := skipCordaTypes(dAtA[iNdEx:])
			if err != nil {
				return err
			}
			if (skippy < 0) || (iNdEx+skippy) < 0 {
				return ErrInvalidLengthCordaTypes
			}
			if (iNdEx + skippy) > l {
				return io.ErrUnexpectedEOF
			}
			iNdEx += skippy
		}
	}

	if iNdEx > l {
		return io.ErrUnexpectedEOF
	}
	return nil
}
func (m *Party) Unmarshal(dAtA []byte) error {
	l := len(dAtA)
	iNdEx := 0
	for iNdEx < l {
		preIndex := iNdEx
		var wire uint64
		for shift := uint(0); ; shift += 7 {
			if shift >= 64 {
				return ErrIntOverflowCordaTypes
			}
			if iNdEx >= l {
				return io.ErrUnexpectedEOF
			}
			b := dAtA[iNdEx]
			iNdEx++
			wire |= uint64(b&0x7F) << shift
			if b < 0x80 {
				break
			}
		}
		fieldNum := int32(wire >> 3)
		wireType := int(wire & 0x7)
		if wireType == 4 {
			return fmt.Errorf("proto: Party: wiretype end group for non-group")
		}
		if fieldNum <= 0 {
			return fmt.Errorf("proto: Party: illegal tag %d (wire type %d)", fieldNum, wire)
		}
		switch fieldNum {
		case 1:
			if wireType != 2 {
				return fmt.Errorf("proto: wrong wireType = %d for field Name", wireType)
			}
			var msglen int
			for shift := uint(0); ; shift += 7 {
				if shift >= 64 {
					return ErrIntOverflowCordaTypes
				}
				if iNdEx >= l {
					return io.ErrUnexpectedEOF
				}
				b := dAtA[iNdEx]
				iNdEx++
				msglen |= int(b&0x7F) << shift
				if b < 0x80 {
					break
				}
			}
			if msglen < 0 {
				return ErrInvalidLengthCordaTypes
			}
			postIndex := iNdEx + msglen
			if postIndex < 0 {
				return ErrInvalidLengthCordaTypes
			}
			if postIndex > l {
				return io.ErrUnexpectedEOF
			}
			if m.Name == nil {
				m.Name = &CordaX500Name{}
			}
			if err := m.Name.Unmarshal(dAtA[iNdEx:postIndex]); err != nil {
				return err
			}
			iNdEx = postIndex
		case 2:
			if wireType != 2 {
				return fmt.Errorf("proto: wrong wireType = %d for field OwningKey", wireType)
			}
			var msglen int
			for shift := uint(0); ; shift += 7 {
				if shift >= 64 {
					return ErrIntOverflowCordaTypes
				}
				if iNdEx >= l {
					return io.ErrUnexpectedEOF
				}
				b := dAtA[iNdEx]
				iNdEx++
				msglen |= int(b&0x7F) << shift
				if b < 0x80 {
					break
				}
			}
			if msglen < 0 {
				return ErrInvalidLengthCordaTypes
			}
			postIndex := iNdEx + msglen
			if postIndex < 0 {
				return ErrInvalidLengthCordaTypes
			}
			if postIndex > l {
				return io.ErrUnexpectedEOF
			}
			if m.OwningKey == nil {
				m.OwningKey = &PublicKey{}
			}
			if err := m.OwningKey.Unmarshal(dAtA[iNdEx:postIndex]); err != nil {
				return err
			}
			iNdEx = postIndex
		default:
			iNdEx = preIndex
			skippy, err := skipCordaTypes(dAtA[iNdEx:])
			if err != nil {
				return err
			}
			if (skippy < 0) || (iNdEx+skippy) < 0 {
				return ErrInvalidLengthCordaTypes
			}
			if (iNdEx + skippy) > l {
				return io.ErrUnexpectedEOF
			}
			iNdEx += skippy
		}
	}

	if iNdEx > l {
		return io.ErrUnexpectedEOF
	}
	return nil
}
func skipCordaTypes(dAtA []byte) (n int, err error) {
	l := len(dAtA)
	iNdEx := 0
	depth := 0
	for iNdEx < l {
		var wire uint64
		for shift := uint(0); ; shift += 7 {
			if shift >= 64 {
				return 0, ErrIntOverflowCordaTypes
			}
			if iNdEx >= l {
				return 0, io.ErrUnexpectedEOF
			}
			b := dAtA[iNdEx]
			iNdEx++
			wire |= (uint64(b) & 0x7F) << shift
			if b < 0x80 {
				break
			}
		}
		wireType := int(wire & 0x7)
		switch wireType {
		case 0:
			for shift := uint(0); ; shift += 7 {
				if shift >= 64 {
					return 0, ErrIntOverflowCordaTypes
				}
				if iNdEx >= l {
					return 0, io.ErrUnexpectedEOF
				}
				iNdEx++
				if dAtA[iNdEx-1] < 0x80 {
					break
				}
			}
		case 1:
			iNdEx += 8
		case 2:
			var length int
			for shift := uint(0); ; shift += 7 {
				if shift >= 64 {
					return 0, ErrIntOverflowCordaTypes
				}
				if iNdEx >= l {
					return 0, io.ErrUnexpectedEOF
				}
				b := dAtA[iNdEx]
				iNdEx++
				length |= (int(b) & 0x7F) << shift
				if b < 0x80 {
					break
				}
			}
			if length < 0 {
				return 0, ErrInvalidLengthCordaTypes
			}
			iNdEx += length
		case 3:
			depth++
		case 4:
			if depth == 0 {
				return 0, ErrUnexpectedEndOfGroupCordaTypes
			}
			depth--
		case 5:
			iNdEx += 4
		default:
			return 0, fmt.Errorf("proto: illegal wireType %d", wireType)
		}
		if iNdEx < 0 {
			return 0, ErrInvalidLengthCordaTypes
		}
		if depth == 0 {
			return iNdEx, nil
		}
	}
	return 0, io.ErrUnexpectedEOF
}

var (
	ErrInvalidLengthCordaTypes        = fmt.Errorf("proto: negative length found during unmarshaling")
	ErrIntOverflowCordaTypes          = fmt.Errorf("proto: integer overflow")
	ErrUnexpectedEndOfGroupCordaTypes = fmt.Errorf("proto: unexpected end of group")
)
