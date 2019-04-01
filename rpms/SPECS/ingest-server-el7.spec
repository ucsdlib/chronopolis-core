%define __jar_repack {%nil}
%define _prefix %{_usr}/local/chronopolis/ingest
%define jar ingest-server.jar
%define yaml application.yml
%define prep ingest-prepare
%define service /usr/lib/systemd/system/ingest-server.service
%define build_time %(date +"%Y%m%d")

Name: ingest-server
Version: %{ver}
Release: %{build_time}.el7
Source: ingest-server.service
Source1: ingest-server.jar
Source2: ingest-application.yml
Source3: ingest-prepare.sh
Summary: Chronopolis Ingest Server
License: BSD-3
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

%preun

systemctl disable ingest-server

%install

%__install -D -m0644 "%{SOURCE0}" "%{buildroot}%{service}"
%__install -D -m0644 "%{SOURCE1}" "%{buildroot}%{_prefix}/%{jar}"
%__install -D -m0644 "%{SOURCE2}" "%{buildroot}%{_prefix}/%{yaml}"
%__install -D -m0755 "%{SOURCE3}" "%{buildroot}%{_prefix}/%{prep}"

%files

%defattr(-,root,root)

%dir %{_prefix}

%{service}
%{_prefix}/%{jar}
%{_prefix}/%{prep}
%config(noreplace) %{_prefix}/%{yaml}

%changelog

* Tue Mar 5 2019 Mike Ritter <shake@umiacs.umd.edu> 3.1.0-20190305
- Set license to BSD 3 clause

* Fri Dec 1 2017 Mike Ritter <shake@umiacs.umd.edu> 2.0.5-20171201
- fix default modebits for prepare script

* Wed Nov 8 2017 Mike Ritter <shake@umiacs.umd.edu> 2.0.3-20171108
- add ingest-prepare script
- remove install commands for logging directory

* Mon Oct 2 2017 Mike Ritter <shake@umiacs.umd.edu> 1.6.0-20171002
- added changelog entry
- update mod for application yaml
