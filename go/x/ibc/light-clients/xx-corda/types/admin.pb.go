// Code generated by protoc-gen-gogo. DO NOT EDIT.
// source: ibc/lightclients/corda/v1/admin.proto

package types

import (
	context "context"
	fmt "fmt"
	grpc1 "github.com/gogo/protobuf/grpc"
	proto "github.com/gogo/protobuf/proto"
	grpc "google.golang.org/grpc"
	codes "google.golang.org/grpc/codes"
	status "google.golang.org/grpc/status"
	emptypb "google.golang.org/protobuf/types/known/emptypb"
	math "math"
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

func init() {
	proto.RegisterFile("ibc/lightclients/corda/v1/admin.proto", fileDescriptor_3cbb05eface553b8)
}

var fileDescriptor_3cbb05eface553b8 = []byte{
	// 234 bytes of a gzipped FileDescriptorProto
	0x1f, 0x8b, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0xff, 0x74, 0x8f, 0xb1, 0x4a, 0xc4, 0x40,
	0x10, 0x40, 0x93, 0x46, 0x24, 0x58, 0xa5, 0x10, 0x3c, 0x61, 0x0b, 0xc1, 0x32, 0x33, 0x9c, 0xb6,
	0x36, 0x1e, 0x88, 0x8d, 0xc5, 0xc1, 0x75, 0x76, 0xbb, 0x9b, 0x75, 0x33, 0x90, 0x64, 0x42, 0x6e,
	0x12, 0x73, 0x7f, 0xe1, 0x67, 0x59, 0x5e, 0x69, 0x29, 0xc9, 0x8f, 0x48, 0xb2, 0x9c, 0xd8, 0xd8,
	0x0e, 0xef, 0xcd, 0xcc, 0x4b, 0x6e, 0xc9, 0x58, 0x2c, 0xc9, 0x17, 0x62, 0x4b, 0x72, 0xb5, 0xec,
	0xd1, 0x72, 0x9b, 0x6b, 0xec, 0xd7, 0xa8, 0xf3, 0x8a, 0x6a, 0x68, 0x5a, 0x16, 0x4e, 0xaf, 0xc8,
	0x58, 0xf8, 0x8b, 0xc1, 0x82, 0x41, 0xbf, 0x5e, 0x5d, 0x7b, 0x66, 0x5f, 0x3a, 0x5c, 0x40, 0xd3,
	0xbd, 0xa1, 0xab, 0x1a, 0x39, 0x04, 0xef, 0xee, 0x25, 0xb9, 0x78, 0x9c, 0xd7, 0xec, 0x5c, 0xdb,
	0x93, 0x75, 0xe9, 0x43, 0x72, 0xbe, 0x2b, 0x3a, 0xc9, 0xf9, 0xbd, 0x4e, 0x2f, 0x21, 0x98, 0x70,
	0x32, 0xe1, 0x69, 0x36, 0x57, 0xff, 0xcc, 0x6f, 0xa2, 0x0d, 0x7f, 0x8e, 0x2a, 0x3e, 0x8e, 0x2a,
	0xfe, 0x1e, 0x55, 0xfc, 0x31, 0xa9, 0xe8, 0x38, 0xa9, 0xe8, 0x6b, 0x52, 0xd1, 0x26, 0x59, 0xae,
	0x6c, 0x67, 0x61, 0x1b, 0xbf, 0x3e, 0x7b, 0x92, 0xa2, 0x33, 0x60, 0xb9, 0xc2, 0x5c, 0x8b, 0xb6,
	0x85, 0xa6, 0xba, 0xd4, 0x26, 0xb4, 0x65, 0x73, 0xb2, 0x67, 0x1c, 0xf0, 0xb7, 0x3d, 0x3b, 0xc5,
	0x0f, 0x43, 0x16, 0xfa, 0xe5, 0xd0, 0xb8, 0xbd, 0x39, 0x5b, 0x5e, 0xb8, 0xff, 0x09, 0x00, 0x00,
	0xff, 0xff, 0x2a, 0xf7, 0xc0, 0x6c, 0x26, 0x01, 0x00, 0x00,
}

// Reference imports to suppress errors if they are not otherwise used.
var _ context.Context
var _ grpc.ClientConn

// This is a compile-time assertion to ensure that this generated file
// is compatible with the grpc package it is being compiled against.
const _ = grpc.SupportPackageIsVersion4

// AdminServiceClient is the client API for AdminService service.
//
// For semantics around ctx use and closing/ending streaming RPCs, please refer to https://godoc.org/google.golang.org/grpc#ClientConn.NewStream.
type AdminServiceClient interface {
	Shutdown(ctx context.Context, in *emptypb.Empty, opts ...grpc.CallOption) (*emptypb.Empty, error)
}

type adminServiceClient struct {
	cc grpc1.ClientConn
}

func NewAdminServiceClient(cc grpc1.ClientConn) AdminServiceClient {
	return &adminServiceClient{cc}
}

func (c *adminServiceClient) Shutdown(ctx context.Context, in *emptypb.Empty, opts ...grpc.CallOption) (*emptypb.Empty, error) {
	out := new(emptypb.Empty)
	err := c.cc.Invoke(ctx, "/ibc.lightclients.corda.v1.AdminService/Shutdown", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

// AdminServiceServer is the server API for AdminService service.
type AdminServiceServer interface {
	Shutdown(context.Context, *emptypb.Empty) (*emptypb.Empty, error)
}

// UnimplementedAdminServiceServer can be embedded to have forward compatible implementations.
type UnimplementedAdminServiceServer struct {
}

func (*UnimplementedAdminServiceServer) Shutdown(ctx context.Context, req *emptypb.Empty) (*emptypb.Empty, error) {
	return nil, status.Errorf(codes.Unimplemented, "method Shutdown not implemented")
}

func RegisterAdminServiceServer(s grpc1.Server, srv AdminServiceServer) {
	s.RegisterService(&_AdminService_serviceDesc, srv)
}

func _AdminService_Shutdown_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(emptypb.Empty)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(AdminServiceServer).Shutdown(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ibc.lightclients.corda.v1.AdminService/Shutdown",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(AdminServiceServer).Shutdown(ctx, req.(*emptypb.Empty))
	}
	return interceptor(ctx, in, info, handler)
}

var _AdminService_serviceDesc = grpc.ServiceDesc{
	ServiceName: "ibc.lightclients.corda.v1.AdminService",
	HandlerType: (*AdminServiceServer)(nil),
	Methods: []grpc.MethodDesc{
		{
			MethodName: "Shutdown",
			Handler:    _AdminService_Shutdown_Handler,
		},
	},
	Streams:  []grpc.StreamDesc{},
	Metadata: "ibc/lightclients/corda/v1/admin.proto",
}
