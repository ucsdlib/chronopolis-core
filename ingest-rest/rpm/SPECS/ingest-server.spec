%define __jar_repack {%nil}
%define _prefix %{_usr}/local/chronopolis/ingest
%define jar ingest-server.jar
%define yaml application.yml
%define initsh /etc/init.d/ingest-server
%define build_time %(date +"%Y%m%d")

Name: ingest-server
Version: %{ver}
Release: %{build_time}%{?dist}
Source: ingest-server.jar
Source1: ingest-server.sh
Source2: application.yml
Summary: Chronopolis Ingest Server
License: UMD
URL: https://gitlab.umiacs.umd.edu/chronopolis
Group: System Environment/Daemons
Requires: postgresql-server >= 8.1
autoprov: yes
autoreq: yes
BuildArch: noarch
BuildRoot: ${_tmppath}/build-%{name}-%{version}

%description
The Ingest Server hosts the API for handling bags, transfers, and
tokens.

%install

%__install -D -m0644 "%{SOURCE0}" "%{buildroot}%{_prefix}/%{jar}"
%__install -D -m0600 "%{SOURCE2}" "%{buildroot}%{_prefix}/%{yaml}"
%__install -D -m0755 "%{SOURCE1}" "%{buildroot}%{initsh}"

%__install -d "%{buildroot}/var/log/chronopolis"

%files

%defattr(-,root,root)
%dir %{_prefix}
%{_prefix}/%{jar}
%config %{_prefix}/%{yaml}
%config %{initsh}

%dir %attr(0755,-,-) /var/log/chronopolis
